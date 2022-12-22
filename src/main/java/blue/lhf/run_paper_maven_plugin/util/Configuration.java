package blue.lhf.run_paper_maven_plugin.util;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.slf4j.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.*;

public class Configuration {
    private Configuration() {
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("run-paper");

    public static Path getServerPath(final @Nullable MavenProject project, final String serverDirectory) throws MojoExecutionException {
        return obtainServerDirectory(project, serverDirectory).resolve("server.jar");
    }

    public static Path getOutputDirectory(final @Nullable MavenProject project) {
        return Path.of(project != null ? project.getBuild().getDirectory() : "target");
    }

    public static Path getServerDirectory(final @Nullable MavenProject project, final String serverDirectory) {
        return getOutputDirectory(project).resolve(serverDirectory);
    }

    public static Path obtainServerDirectory(final @Nullable MavenProject project,
                                             final String serverDirectory) throws MojoExecutionException {

        final Path path = getServerDirectory(project, serverDirectory);

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create server directory at %s".formatted(path), e);
        }

        return path;
    }
}
