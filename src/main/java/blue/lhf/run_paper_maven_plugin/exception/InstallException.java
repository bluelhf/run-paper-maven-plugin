package blue.lhf.run_paper_maven_plugin.exception;

public class InstallException extends RuntimeException {
    public InstallException() {
    }

    public InstallException(String message) {
        super(message);
    }

    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstallException(Throwable cause) {
        super(cause);
    }

    public InstallException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
