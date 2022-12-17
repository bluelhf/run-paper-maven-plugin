package blue.lhf.run_paper_maven_plugin.util;

import org.slf4j.event.Level;

import javax.annotation.Nullable;
import java.util.*;

import static blue.lhf.run_paper_maven_plugin.util.LoggingUtility.log;

public class Progressive implements AutoCloseable {

    @Nullable
    private final Long maximum;
    private final String prompt;
    private final Level logLevel;

    private long progress = 0;
    private final Timer timer = new Timer();

    private Progressive(final String prompt) {
        this(Level.INFO, prompt);
    }

    private Progressive(Level logLevel, final String prompt) {
        this(logLevel, prompt, null);
    }

    private Progressive(@Nullable final Long maximum, final String prompt) {
        this(Level.INFO, prompt, maximum);
    }

    private Progressive(Level logLevel, final String prompt, @Nullable final Long maximum) {
        this.logLevel = logLevel;
        this.maximum = maximum;
        this.prompt = prompt;

        log(logLevel, "");
        display();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                display();
            }
        }, 333, 333);
    }

    public static Progressive ofSize(final long maximum, final String prompt) {
        return ofSize(Level.INFO, prompt, maximum);
    }

    public static Progressive ofSize(Level logLevel, final String prompt, final long maximum) {
        return new Progressive(logLevel, prompt, maximum);
    }

    public static Progressive ofSize(final OptionalLong maximum, final String prompt) {
        return ofSize(Level.INFO, maximum, prompt);
    }

    public static Progressive ofSize(Level logLevel, final OptionalLong maximum, final String prompt) {
        return new Progressive(logLevel, prompt, maximum.isPresent() ? maximum.getAsLong() : null);
    }

    public void addProgress(final long addition) {
        this.progress += addition;
    }

    public void setProgress(final long progress) {
        this.progress = progress;
    }

    public long getProgress() {
        return progress;
    }

    @Nullable
    public Long getMaximum() {
        return maximum;
    }

    protected void display() {
        final StringBuilder builder = new StringBuilder("\033[2K\033[1A\033[0K%s ".formatted(prompt));
        if (maximum != null) {
            final int maxLength = (int) Math.floor(Math.log10(maximum)) + 1;
            final String zpn = "%0" + maxLength + "d";

            builder
                .append("(")
                .append(zpn.formatted(progress)).append("/").append(zpn.formatted(maximum))
                .append(" | ")
                .append("%06.2f".formatted(progress / (0.01 * maximum))).append(" %")
                .append(")");

        } else {
            builder.append("(%s)".formatted(progress));
        }

        log(this.logLevel, builder.toString());
    }

    @Override
    public void close() {
        timer.cancel();
        display();
    }
}
