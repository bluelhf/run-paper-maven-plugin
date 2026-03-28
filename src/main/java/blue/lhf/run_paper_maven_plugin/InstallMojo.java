package blue.lhf.run_paper_maven_plugin;

import blue.lhf.run_paper_maven_plugin.exception.InstallException;
import blue.lhf.run_paper_maven_plugin.model.Download;
import blue.lhf.run_paper_maven_plugin.model.paper.PaperAPI;
import blue.lhf.run_paper_maven_plugin.util.Configuration;
import blue.lhf.run_paper_maven_plugin.util.HotswapConfiguration;
import blue.lhf.run_paper_maven_plugin.util.HotswapDownloader;
import blue.lhf.run_paper_maven_plugin.util.Progressive;
import blue.lhf.run_paper_maven_plugin.util.ProgressiveTransferListener;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.slf4j.event.Level;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static blue.lhf.run_paper_maven_plugin.util.Configuration.LOGGER;
import static org.apache.maven.plugins.annotations.InstantiationStrategy.SINGLETON;

@Mojo(name = "install", instantiationStrategy = SINGLETON, requiresOnline = true, threadSafe = true)
public class InstallMojo extends AbstractMojo {

    @Inject
    private HotswapDownloader downloader;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.pluginArtifactRepositories}", readonly = true)
    private List<MavenArtifactRepository> pluginRepositories;

    @Parameter(property = "minecraftVersion", required = true)
    protected String minecraftVersion;

    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(name = "serverDirectory", defaultValue = "mc_server")
    protected String serverDirectory;

    @Parameter(name = "hotswap")
    protected HotswapConfiguration hotswap = HotswapConfiguration.disabled();

    @Override
    public void execute() throws MojoExecutionException {
        PaperAPI.get().fetchBuilds(minecraftVersion).thenCompose(builds ->
            PaperAPI.get().fetchApplication(builds.last()))
            .thenAcceptAsync(this::acceptJAR)
            .exceptionally(exception -> {
                throw new RuntimeException(new MojoExecutionException("An exception occurred while downloading the application JAR", exception));
            }).join();

        if (hotswap.isEnabled()) {
            LOGGER.info("Requested hot-swap, downloading...");
            try {
                final Path outputDirectory = Configuration.getHotswapDirectory(project, serverDirectory, hotswap);
                final List<RemoteRepository> remotes = new ArrayList<>();
                for (final MavenArtifactRepository mavenArtifactRepository : pluginRepositories) {
                    remotes.add(RepositoryUtils.toRepo(mavenArtifactRepository));
                }
                final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repoSession);
                session.setTransferListener(new ProgressiveTransferListener());
                downloader.download(session, hotswap, outputDirectory, remotes);
            } catch (VersionRangeResolutionException | ArtifactResolutionException | IOException e) {
                throw new MojoExecutionException("An exception occurred while downloading the runtime", e);
            }
        }
    }

    protected static String sha256(final InputStream stream, Consumer<Integer> onUpdate) throws NoSuchAlgorithmException, IOException {
        final MessageDigest message = MessageDigest.getInstance("SHA-256");
        try (final InputStream is = stream) {
            byte[] buffer = new byte[16777216];

            int read;
            while ((read = is.read(buffer)) != -1) {
                if (onUpdate != null) onUpdate.accept(read);
                message.update(buffer, 0, read);
            }
        }

        return stringify(message.digest());
    }

    protected boolean checkHash(final Path localPath, final String remote) {
        if (Files.notExists(localPath)) return false;
        try {

            final long size = Files.size(localPath);
            final String local;
            try (final Progressive progressive = Progressive.ofSize(Level.DEBUG,
                    "Computing local application hash...", size)) {
                local = sha256(Files.newInputStream(localPath), progressive::addProgress);
            }

            LOGGER.debug("Local  application JAR has SHA-256 hash: %s".formatted(local));
            LOGGER.debug("Remote application JAR has SHA-256 hash: %s".formatted(remote));

            if (remote.equals(local)) {
                LOGGER.debug("Hash for local application JAR matched remote, skipping download.");
                return true;
            } else {
                LOGGER.info("Hashes do not match, downloading new JAR...");
                return false;
            }

        } catch (IOException e) {
            LOGGER.warn("Could not open local application JAR, skipping hash check.");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Could not initialise SHA-256 digest, skipping hash check.");
        }

        return false;
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
    private static String stringify(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        return sb.toString();
    }

    protected void acceptJAR(final Download download) {
        try (final InputStream input = download.stream()) {
            final Path targetPath = Configuration.getServerPath(project, serverDirectory);
            if (download.sha256().isPresent() && checkHash(targetPath, download.sha256().get())) {
                return;
            }

            try (
                final OutputStream output = Files.newOutputStream(targetPath);
                final Progressive progressive = Progressive.ofSize(
                    download.length(), "Downloading application archive...")
            ) {
                final byte[] buffer = new byte[16777216];

                int read;
                while ((read = input.read(buffer)) != -1) {
                    progressive.addProgress(read);
                    output.write(buffer, 0, read);
                }
            }

        } catch (IOException | MojoExecutionException exception) {
            throw new InstallException("Failed to download application JAR", exception);
        }
    }
}