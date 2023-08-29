package xyz.destiall.sgcraftlink.bungeeproviders;

import xyz.destiall.sgcraftlink.providers.LoggerProvider;

import java.util.logging.Logger;

public class BungeeLogger implements LoggerProvider {
    private final Logger logger;

    public BungeeLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warning(String msg) {
        logger.warning(msg);
    }

    @Override
    public void error(String msg) {
        logger.severe(msg);
    }
}
