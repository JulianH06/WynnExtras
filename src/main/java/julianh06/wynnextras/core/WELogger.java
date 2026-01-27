package julianh06.wynnextras.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WELogger {
    public Logger LOGGER;

    public WELogger(String modId) {
        this.LOGGER = LoggerFactory.getLogger(modId);
    }

    public void logInfo(String msg) {
        LOGGER.info(msg);
    }

    public void logInfo(String msg, Object... args) {
        LOGGER.info(msg, args);
    }

    public void logWarn(String msg) {
        LOGGER.warn(msg);
    }

    public void logDebug(String msg) {
        LOGGER.debug(msg);
    }

    public void logError(String msg) {
        LOGGER.error(msg);
    }

    public void logError(String msg, Object... args) {
        LOGGER.error(msg, args);
    }

    public void logError(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }
}
