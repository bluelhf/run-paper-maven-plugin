package blue.lhf.run_paper_maven_plugin.util;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

public class ProgressiveTransferListener extends AbstractTransferListener {
    final Map<TransferResource, Progressive> progressiveMap = new HashMap<>();

    @Override
    public void transferStarted(TransferEvent event) {
        progressiveMap.putIfAbsent(event.getResource(), createProgress(event));
    }

    @NonNullDecl
    private static Progressive createProgress(final TransferEvent event) {
        return Progressive.ofSize(Level.INFO, "Downloading artifact: " + event.getResource().getResourceName(),
            event.getResource().getContentLength());
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        progressiveMap.computeIfAbsent(event.getResource(), (r) -> createProgress(event))
            .setProgress(event.getTransferredBytes());
    }

    @Override
    public void transferFailed(TransferEvent event) {
        closeTransfer(event);
    }

    private void closeTransfer(TransferEvent event) {
        progressiveMap.computeIfPresent(event.getResource(), (resource, progressive) -> {
            progressive.close();
            return null;
        });
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        closeTransfer(event);
    }
}