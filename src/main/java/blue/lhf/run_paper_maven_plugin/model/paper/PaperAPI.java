package blue.lhf.run_paper_maven_plugin.model.paper;

import blue.lhf.run_paper_maven_plugin.model.API;
import blue.lhf.run_paper_maven_plugin.model.exception.*;
import com.vdurmont.semver4j.Semver;

import java.io.OutputStream;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class PaperAPI implements API<Build> {
    @Override
    public CompletableFuture<TreeSet<Build>> fetchBuilds(Semver version) throws APIException, InvalidVersionException {
        return null;
    }

    @Override
    public void fetchJAR(Build build, OutputStream stream) {

    }
}
