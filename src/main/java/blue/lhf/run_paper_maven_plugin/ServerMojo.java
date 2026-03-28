package blue.lhf.run_paper_maven_plugin;

import blue.lhf.run_paper_maven_plugin.util.Configuration;
import blue.lhf.run_paper_maven_plugin.util.HotswapConfiguration;
import blue.lhf.run_paper_maven_plugin.util.HotswapDownloader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "run-server", requiresProject = false, threadSafe = true)
public class ServerMojo extends AbstractMojo {

    protected static final List<String> JVM_DEFAULTS = List.of(
        "-Xms10G",
        "-Xmx10G",
        "-XX:+UseG1GC",
        "-XX:+ParallelRefProcEnabled",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+DisableExplicitGC",
        "-XX:+AlwaysPreTouch",
        "-XX:G1NewSizePercent=30",
        "-XX:G1MaxNewSizePercent=40",
        "-XX:G1HeapRegionSize=8M",
        "-XX:G1ReservePercent=20",
        "-XX:G1HeapWastePercent=5",
        "-XX:G1MixedGCCountTarget=4",
        "-XX:InitiatingHeapOccupancyPercent=15",
        "-XX:G1MixedGCLiveThresholdPercent=90",
        "-XX:G1RSetUpdatingPauseTimePercent=5",
        "-XX:SurvivorRatio=32",
        "-XX:MaxTenuringThreshold=1",
        "-Dusing.aikars.flags=https://mcflags.emc.gs",
        "-Daikars.new.flags=true",
        "-Ddisable.watchdog=true"
    );

    protected static final List<String> SERVER_DEFAULTS = List.of("--nogui");

    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(name = "serverDirectory", defaultValue = "mc_server")
    protected String serverDirectory;

    @Parameter(name = "jvmFlags")
    protected String[] jvmFlags = new String[0];

    @Parameter(name = "includeDefaultJvmFlags", defaultValue = "true")
    protected boolean includeDefaultJvmFlags = true;

    @Parameter(name = "acceptEula", defaultValue = "false")
    protected boolean acceptEula;

    @Parameter(name = "serverFlags")
    protected String[] serverFlags = new String[0];

    @Parameter(name = "includeDefaultServerFlags", defaultValue = "true")
    protected boolean includeDefaultServerFlags = true;

    @Parameter(name = "pluginPath", defaultValue = "${project.build.finalName}.jar")
    protected String[] pluginPath = new String[] {"${project.build.finalName}.jar"};

    @Inject
    private HotswapDownloader downloader;

    @Parameter(name = "hotswap")
    protected HotswapConfiguration hotswap = HotswapConfiguration.disabled();

    public List<String> getJvmBaseFlags() {
        final List<String> withoutHotswap = includeDefaultJvmFlags ? JVM_DEFAULTS : List.of();
        final List<String> withHotswap = new ArrayList<>(withoutHotswap);
        if (hotswap.isEnabled()) withHotswap.add(hotswap.getDebugFlag());
        return withHotswap;
    }

    public List<String> getServerBaseFlags() {
        return includeDefaultServerFlags ? SERVER_DEFAULTS : List.of();
    }

    @Override
    public void execute() throws MojoExecutionException {
        String javaExecutable = ProcessHandle.current().info().command().orElse("java");
        if (hotswap.isEnabled()) {
            final HotswapDownloader.Result paths = downloader.getPaths(Configuration.getHotswapDirectory(project, serverDirectory, hotswap));
            final Path javaPath;
            try {
                javaPath = paths.findJavaExecutable();
                javaExecutable = javaPath.toAbsolutePath().toString();
            } catch (IOException e) {
                throw new MojoExecutionException("Could not find Java executable", e);
            }
        }
        final List<String> command = new ArrayList<>(List.of(javaExecutable));
        command.addAll(getJvmBaseFlags());
        if (acceptEula)
            command.add("-Dcom.mojang.eula.agree=true");
        command.addAll(Arrays.asList(this.jvmFlags));
        command.add("-jar");
        command.add(Configuration.getServerPath(project, serverDirectory).toAbsolutePath().toString());

        command.addAll(getServerBaseFlags());
        command.addAll(Arrays.asList(this.serverFlags));
        for(String pluginpath : pluginPath) {
            if (pluginpath != null && !pluginpath.equalsIgnoreCase("null")) {
                command.add("--add-plugin");
                command.add(Configuration.getOutputDirectory(project).resolve(pluginpath).toAbsolutePath().toString());
            }
        }

        try {
            final Process process = new ProcessBuilder()
                .command(command).inheritIO()
                .directory(Configuration.obtainServerDirectory(project, serverDirectory).toFile())
                .start();
            process.waitFor();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to run application", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
