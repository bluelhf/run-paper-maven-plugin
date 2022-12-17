package blue.lhf.run_paper_maven_plugin.util;

import org.slf4j.event.Level;

import static blue.lhf.run_paper_maven_plugin.util.Configuration.LOGGER;

public class LoggingUtility {
    private LoggingUtility() {
    }

    public static void log(final Level level, final String message) {
        switch (level) {
            case TRACE -> LOGGER.trace(message);
            case DEBUG -> LOGGER.debug(message);
            case  INFO -> LOGGER. info(message);
            case  WARN -> LOGGER. warn(message);
            case ERROR -> LOGGER.error(message);
        }
    }
}
