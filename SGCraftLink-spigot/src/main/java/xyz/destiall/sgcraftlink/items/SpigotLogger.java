package xyz.destiall.sgcraftlink.items;

import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.providers.LoggerProvider;

import java.util.logging.Logger;

public class SpigotLogger implements LoggerProvider {
    private final Logger logger;
    public SpigotLogger(SGCraftLinkSpigot plugin) {
        this.logger = plugin.getLogger();
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
