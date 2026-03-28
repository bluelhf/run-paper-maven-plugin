package blue.lhf.run_paper_maven_plugin.util;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.version.Version;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Named
@Singleton
public class HotswapDownloader {
    private static final String SYSTEM_CLASSIFIER;
    static {
        SYSTEM_CLASSIFIER = System.getProperty("os.name").toLowerCase().transform(os -> {
            if (os.contains("win")) return "windows";
            if (os.contains("mac")) return "osx";
            return "linux";
        }) + "-" + System.getProperty("os.arch").toLowerCase().transform(arch -> {
            if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
            return "x64";
        });
    }

    private final RepositorySystem repoSystem;
    private final ArtifactRepositoryLayout defaultLayout;

    @Inject
    public HotswapDownloader(RepositorySystem system, @Named("default") ArtifactRepositoryLayout defaultLayout) {
        this.repoSystem = system;
        this.defaultLayout = defaultLayout;
    }

    public record Result(Path javaHome, Path agentJar) {
        public Path findJavaExecutable() throws IOException {
            final AtomicReference<Path> pathReference = new AtomicReference<>();
            Files.walkFileTree(javaHome, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    final boolean isInBin = StreamSupport.stream(file.spliterator(), false)
                        .anyMatch(path -> path.getFileName().toString().equalsIgnoreCase("bin"));
                    final String filename = file.getFileName().toString().toLowerCase();
                    final boolean isJava = filename.equals("java") || filename.equals("java.exe");
                    if (isInBin && isJava) {
                        pathReference.set(file);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
            if (Objects.isNull(pathReference.get())) throw new NoSuchFileException(
                "No file was in " + javaHome.toAbsolutePath() + " was in 'bin' and named 'java' or 'java.exe'");
            return pathReference.get();
        }
    }

    public Result getPaths(final Path outputDirectory) {
        return new Result(
            getRuntimePath(outputDirectory),
            getAgentPath(outputDirectory)
        );
    }

    public void download(final RepositorySystemSession session,
                         final HotswapConfiguration configuration,
                         final Path outputDirectory,
                         final List<RemoteRepository> repos)
        throws VersionRangeResolutionException, ArtifactResolutionException, IOException {

        configuration.initialise(defaultLayout);

        final List<RemoteRepository> repositories = new ArrayList<>(repos);
        for (final ArtifactRepository repository : configuration.getRepositories())
            repositories.add(RepositoryUtils.toRepo(repository));

        final File zippedRuntime = downloadRuntimeTarball(session, configuration, repositories);
        final TarGZipUnArchiver ua = new TarGZipUnArchiver();
        ua.setSourceFile(zippedRuntime);
        final Path jbrDirectory = getRuntimePath(outputDirectory);
        final Path extractDirectory = jbrDirectory.resolve(".tmp");
        Files.createDirectories(extractDirectory);
        ua.setDestDirectory(extractDirectory.toFile());
        ua.extract();

        try (final Stream<Path> files = Files.list(extractDirectory)) {
            final List<Path> paths = files.toList();
            for (final Path tar : paths) {
                try (final Stream<Path> tree = Files.list(tar)) {
                    for (final Path root : tree.toList()) {
                        final Path targetRoot = jbrDirectory.resolve(root.getFileName());
                        Files.walkFileTree(root, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                final Path targetPath = targetRoot.resolve(root.relativize(file));
                                Files.createDirectories(targetPath.getParent());
                                Files.move(file, targetPath, REPLACE_EXISTING);
                                Files.deleteIfExists(file);
                                return super.visitFile(file, attrs);
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.deleteIfExists(dir);
                                return super.postVisitDirectory(dir, exc);
                            }
                        });
                    }
                }
                Files.deleteIfExists(tar);
            }
        }
        Files.deleteIfExists(extractDirectory);
        final Path agentPath = getAgentPath(outputDirectory);
        Files.copy(downloadAgentJar(session, configuration, repositories).toPath(), agentPath, REPLACE_EXISTING);
    }

    private static Path getRuntimePath(final Path outputDirectory) {
        return outputDirectory.resolve("jbr");
    }

    private static Path getAgentPath(Path outputDirectory) {
        return outputDirectory.resolve("hotswap-agent.jar");
    }

    private File downloadAgentJar(RepositorySystemSession session, HotswapConfiguration configuration, List<RemoteRepository> repositories) throws VersionRangeResolutionException, ArtifactResolutionException {
        final Artifact artifact = RepositoryUtils.toDependency(configuration.getAgent(), session.getArtifactTypeRegistry()).getArtifact();
        final VersionRangeRequest versionRequest = new VersionRangeRequest(artifact, repositories, null);
        final VersionRangeResult versionResult = repoSystem.resolveVersionRange(session, versionRequest);
        final Version version = versionResult.getHighestVersion();

        final Artifact reversioned = artifact.setVersion(version.toString());
        final ArtifactRequest request = new ArtifactRequest(reversioned, repositories, null);
        final ArtifactResult result = repoSystem.resolveArtifact(session, request);
        return result.getArtifact().getFile();
    }

    private File downloadRuntimeTarball(RepositorySystemSession session, HotswapConfiguration configuration, List<RemoteRepository> repositories) throws VersionRangeResolutionException, ArtifactResolutionException {
        final Artifact artifact = RepositoryUtils.toDependency(configuration.getRuntime(), session.getArtifactTypeRegistry()).getArtifact();
        final VersionRangeRequest versionRequest = new VersionRangeRequest(artifact, repositories, null);
        final VersionRangeResult versionResult = repoSystem.resolveVersionRange(session, versionRequest);
        final Version version = versionResult.getHighestVersion();

        final Artifact reversioned = artifact.setVersion(version.toString());
        final Artifact classified = new SubArtifact(reversioned, SYSTEM_CLASSIFIER, "tgz");
        final ArtifactRequest request = new ArtifactRequest(classified, repositories, null);
        final ArtifactResult result = repoSystem.resolveArtifact(session, request);
        return result.getArtifact().getFile();
    }
}
