package julianh06.wynnextras.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WELogger {
    private final Logger logger;

    public WELogger(String modId) {
        this.logger = LoggerFactory.getLogger(modId);
    }

    public void logInfo(String msg) {
        logger.info(msg);
    }

    public void logInfo(String msg, Object... args) {
        logger.info(msg, args);
    }

    public void logWarn(String msg) {
        logger.warn(msg);
    }

    public void logDebug(String msg) {
        logger.debug(msg);
    }

    public void logError(String msg) {
        logger.error(msg);
    }

    public void logError(String msg, Object... args) {
        logger.error(msg, args);
    }

    public void logError(String msg, Throwable t) {
        logger.error(msg, t);
    }
}
