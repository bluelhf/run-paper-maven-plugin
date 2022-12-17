package blue.lhf.run_paper_maven_plugin.model.paper;

import com.vdurmont.semver4j.Semver;

public record Build(Semver version, int identifier) implements Comparable<Build> {
    @Override
    public int compareTo(Build o) {
        final int versionComparison = version.compareTo(o.version);
        if (versionComparison != 0) return versionComparison;
        return Long.compare(identifier, o.identifier);
    }
}
