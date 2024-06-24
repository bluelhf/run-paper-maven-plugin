package blue.lhf.run_paper_maven_plugin.model.paper;

import java.util.Objects;

public record Build(String version, int identifier) implements Comparable<Build> {
    @Override
    public int compareTo(Build o) {
        if (!Objects.equals(version, o.version)) throw new IllegalArgumentException("Cannot compare builds of different versions");
        return Long.compare(identifier, o.identifier);
    }
}
