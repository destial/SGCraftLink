package xyz.destiall.sgcraftlink.velocityproviders;

import org.slf4j.Logger;
import xyz.destiall.sgcraftlink.providers.LoggerProvider;

public class VelocityLogger implements LoggerProvider {
    private final Logger logger;

    public VelocityLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warning(String msg) {
        logger.warn(msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }
}
