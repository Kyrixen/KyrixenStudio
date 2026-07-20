package io.kyrixen.studio.ide;

import dev.kyrixen.libs.logger.Logger;

public class JSConsole {

    public void info(String message) {
        Logger.LOGGER.info("JS", message);
    }

    public void debug(String debug) {
        Logger.LOGGER.debug("JS", debug);
    }

    public void warn(String warn) {
        Logger.LOGGER.warn("JS", warn);
    }

    public void error(String error) {
        Logger.LOGGER.error("JS", error);
    }

}