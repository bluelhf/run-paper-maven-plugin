package blue.lhf.run_paper_maven_plugin.model.paper;

import blue.lhf.run_paper_maven_plugin.model.*;
import blue.lhf.run_paper_maven_plugin.exception.APIException;
import com.vdurmont.semver4j.Semver;
import mx.kenzie.argo.*;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;

public class PaperAPI {
    private static final URI BASE = URI.create("https://api.papermc.io/");

    private static final HttpClient client = HttpClient.newHttpClient();

    public static V2 get() {
        return V2.INSTANCE;
    }

    public static class V2 implements API<Build> {
        private static final V2 INSTANCE = new V2();

        private static final URI    LOCAL_BASE =          BASE.resolve("v2/"      );
        private static final URI PROJECTS_BASE =    LOCAL_BASE.resolve("projects/");
        private static final URI    PAPER_BASE = PROJECTS_BASE.resolve("paper/"   );
        private static final URI VERSIONS_BASE =    PAPER_BASE.resolve("versions/");

        protected V2() {
        }

        //region Fetch version information

        public static class VersionInformation {
            public int[] builds = new int[0];
        }

        protected static VersionInformation parseVersionInformation(final HttpResponse<InputStream> response) {
            try (Json json = new Json(response.body())) {
                return json.toObject(VersionInformation.class);
            }
        }

        protected static BuildInformation parseBuildInformation(final HttpResponse<InputStream> response) {
            try (Json json = new Json(response.body())) {
                return json.toObject(BuildInformation.class);
            }
        }

        @Override
        public CompletableFuture<SortedSet<Build>> fetchBuilds(Semver version) throws APIException {
            final HttpRequest request = HttpRequest.newBuilder()
                .GET().uri(VERSIONS_BASE.resolve(version.toString()))
                .build();

            final CompletableFuture<HttpResponse<InputStream>> future = client.sendAsync(request, ofInputStream());
            return future
                .thenApplyAsync(response -> handleErrors(response, status -> {
                    if (status == 404) {
                        return "Failed to retrieve version information for version %s".formatted(version);
                    }

                    return null;
                }))
                .thenApplyAsync(PaperAPI.V2::parseVersionInformation)
                .thenApplyAsync(information -> {
                    final TreeSet<Build> set = new TreeSet<>();
                    for (final int identifier : information.builds) {
                        set.add(new Build(version, identifier));
                    }

                    return set;
                });
        }
        //endregion

        //region Download JAR
        public static class BuildInformation {
            public Map<String, DownloadInformation> downloads = Map.of();
        }

        public static class DownloadInformation {
            public String name = null;
            public String sha256 = null;
        }

        protected URI getURI(final Build build) {
            return VERSIONS_BASE.resolve("%s/builds/%s/".formatted(build.version(), build.identifier()));
        }

        public CompletableFuture<DownloadInformation> fetchInformation(final Build build) throws APIException {
            final URI fetchURI = getURI(build);

            final HttpRequest request = HttpRequest.newBuilder(fetchURI).build();
            return client.sendAsync(request, ofInputStream())
                .thenApply(response -> handleErrors(response, status -> {
                    if (status == 404) {
                        return "Failed to retrieve build information for build %s of %s from %s"
                            .formatted(build.identifier(), build.version(), fetchURI);
                    }

                    return null;
                }))
                .thenApplyAsync(PaperAPI.V2::parseBuildInformation)
                .thenApply(information -> {
                    final DownloadInformation applicationInformation = new DownloadInformation();

                    //noinspection ConstantConditions, unchecked to hack around Argo weirdness
                    final HashMap<String, Object> map = (HashMap<String, Object>) (Object)
                        information.downloads.get("application");

                    try (final Json.JsonHelper json = new Json.JsonHelper()) {
                        json.mapToObject(applicationInformation, DownloadInformation.class, map);
                    }

                    return applicationInformation;
                });
        }

        @Override
        public CompletableFuture<Download> fetchApplication(Build build) throws APIException {
            return fetchInformation(build).thenCompose(information -> {
                final URI downloadURI = getURI(build)
                    .resolve("downloads/")
                    .resolve(information.name);

                final HttpRequest request = HttpRequest.newBuilder(downloadURI).build();
                return client.sendAsync(request, ofInputStream())
                    .thenApply(response -> handleErrors(response, status -> {
                        if (status == 404) {
                            return "Failed to retrieve application JAR for build %s of %s from %s"
                                .formatted(build.identifier(), build.version(), downloadURI);
                        }

                        return null;
                    }))
                    .thenApplyAsync(response -> toDownload(response, information.sha256));
            });
        }
        //endregion

        //region HTTP utilities

        protected static Download toDownload(final HttpResponse<InputStream> response, final String sha256) {
            return new Download(response.body(), response.headers().firstValueAsLong("Content-Length"), Optional.ofNullable(sha256));
        }

        protected static HttpResponse<InputStream> handleErrors(final HttpResponse<InputStream> response, final Function<Integer, String> errorMapping) {
            final int status = response.statusCode();
            final InputStream stream = response.body();
            try {
                if (status < 200 || status >= 400) {
                    final String body = new String(stream.readAllBytes());
                    final String error = errorMapping.apply(status);
                    if (error != null) {
                        throw new APIException(error);
                    }

                    throw new APIException("Unexpected non-OK status code %s: %s".formatted(status, body));
                }
            } catch (IOException e) {
                throw new APIException("An I/O exception occurred while reading the response body for error reporting", e);
            }

            return response;
        }
        //endregion
    }
}
