package blue.lhf.run_paper_maven_plugin.model;

import blue.lhf.run_paper_maven_plugin.exception.APIException;

import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

public interface API<Revision extends Comparable<Revision>> {
    CompletableFuture<SortedSet<Revision>> fetchBuilds(final String version) throws APIException;
    CompletableFuture<Download> fetchApplication(final Revision build) throws APIException;
}
