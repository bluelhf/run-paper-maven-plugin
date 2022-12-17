package blue.lhf.run_paper_maven_plugin.model;

import blue.lhf.run_paper_maven_plugin.exception.APIException;
import com.vdurmont.semver4j.Semver;

import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

public interface API<Revision extends Comparable<Revision>> {
    CompletableFuture<SortedSet<Revision>> fetchBuilds(final Semver version) throws APIException;
    CompletableFuture<Download> fetchApplication(final Revision build) throws APIException;
}
