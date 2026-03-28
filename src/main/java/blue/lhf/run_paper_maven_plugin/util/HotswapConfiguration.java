package blue.lhf.run_paper_maven_plugin.util;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for hot-swapping. If enabled by setting {@link HotswapConfiguration#enabled} to true, downloads
 * a version of the JDK that is more lenient with hot-swapping, and automatically starts a debugger on port 5005.
 * */
public class HotswapConfiguration {
    private static final Dependency DEFAULT_RUNTIME;
    private static final Dependency DEFAULT_AGENT;

    static {
        DEFAULT_RUNTIME = new Dependency();
        DEFAULT_RUNTIME.setGroupId("com.jetbrains.jdk");
        DEFAULT_RUNTIME.setArtifactId("jbr");
        DEFAULT_RUNTIME.setVersion("[0,)");
        DEFAULT_RUNTIME.setType("tgz");

        DEFAULT_AGENT = new Dependency();
        DEFAULT_AGENT.setGroupId("org.hotswapagent");
        DEFAULT_AGENT.setArtifactId("hotswap-agent");
        DEFAULT_AGENT.setVersion("[0,)");
        DEFAULT_AGENT.setType("jar");
    }
    @Parameter
    protected Dependency runtime = DEFAULT_RUNTIME;

    @Component(role = ArtifactRepositoryLayout.class, hint = "default")
    private ArtifactRepositoryLayout defaultLayout;

    @Parameter
    protected Dependency agent = DEFAULT_AGENT;

    @Parameter
    protected String debugFlag = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005";

    @Parameter(defaultValue = ".hotswap")
    protected String outputDirectory = ".hotswap";

    @Parameter
    protected List<ArtifactRepository> repositories;

    public List<ArtifactRepository> getRepositories() {
        if (repositories == null) {
            final List<MavenArtifactRepository> modifiableRepositories = new ArrayList<>();
            {
                final var repository = new MavenArtifactRepository();
                repository.setId("central");
                repository.setUrl("https://repo.maven.apache.org/maven2/");
                repository.setLayout(defaultLayout);
                modifiableRepositories.add(repository);
            }
            {
                final var repository = new MavenArtifactRepository();
                repository.setId("itemis-mps");
                repository.setUrl("https://artifacts.itemis.cloud/repository/maven-mps/");
                repository.setLayout(defaultLayout);
                modifiableRepositories.add(repository);
            }

            this.repositories = Collections.unmodifiableList(modifiableRepositories);
        }

        return this.repositories;
    }

    @Parameter
    protected boolean enabled = true;

    public HotswapConfiguration() { }

    public void set(final String value) {
        this.enabled = Boolean.parseBoolean(value);
    }

    public HotswapConfiguration(final boolean enabled) {
        this.enabled = enabled;
    }

    public static HotswapConfiguration disabled() {
        return new HotswapConfiguration(false);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Dependency getAgent() {
        return agent;
    }

    public Dependency getRuntime() {
        return runtime;
    }

    public void initialise(final ArtifactRepositoryLayout defaultLayout) {
        this.defaultLayout = defaultLayout;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String getDebugFlag() {
        return debugFlag;
    }
}
