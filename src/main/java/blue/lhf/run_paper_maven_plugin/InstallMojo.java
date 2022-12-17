package blue.lhf.run_paper_maven_plugin;

import blue.lhf.run_paper_maven_plugin.exception.InstallException;
import blue.lhf.run_paper_maven_plugin.model.Download;
import blue.lhf.run_paper_maven_plugin.model.paper.PaperAPI;
import blue.lhf.run_paper_maven_plugin.util.*;
import com.google.common.hash.*;
import com.vdurmont.semver4j.*;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.slf4j.event.Level;

import java.io.*;
import java.nio.file.*;

import static blue.lhf.run_paper_maven_plugin.util.Configuration.LOGGER;
import static org.apache.maven.plugins.annotations.InstantiationStrategy.SINGLETON;

@Mojo(name = "install", instantiationStrategy = SINGLETON, requiresOnline = true, threadSafe = true)
public class InstallMojo extends AbstractMojo {

    @Parameter(property = "minecraftVersion", required = true)
    protected String minecraftVersion;

    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(name = "serverDirectory", defaultValue = "mc_server")
    protected String serverDirectory;

    public final Semver parseVersion() throws MojoExecutionException {
        try {
            return new Semver(minecraftVersion);
        } catch (SemverException exception) {
            throw new MojoExecutionException("Invalid version: %s".formatted(minecraftVersion), exception);
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        final Semver version = parseVersion();

        PaperAPI.get().fetchBuilds(version).thenCompose(builds ->
            PaperAPI.get().fetchApplication(builds.last()))
            .thenAcceptAsync(this::acceptJAR)
            .exceptionally(exception -> {
                throw new RuntimeException(new MojoExecutionException("An exception occurred while downloading the application JAR", exception));
            }).join();
    }

    @SuppressWarnings("UnstableApiUsage")
    protected boolean checkHash(final Path localPath, final String remote) {
        if (Files.notExists(localPath)) return false;
        try {
            final Hasher hasher = Hashing.sha256().newHasher();
            final long size = Files.size(localPath);
            try (
                final InputStream stream = Files.newInputStream(localPath);
                final Progressive progressive = Progressive.ofSize(Level.DEBUG,
                    "Computing local application hash...", size)
            ) {
                byte[] buffer = new byte[16777216];

                int read;
                while ((read = stream.read(buffer)) != -1) {
                    progressive.addProgress(read);
                    hasher.putBytes(buffer, 0, read);
                }
            }

            final String local = hasher.hash().toString();

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
        }

        return false;
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