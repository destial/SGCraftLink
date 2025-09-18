package xyz.destiall.sgcraftlink;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.scheduler.Scheduler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SGCraftLinkFabric implements ModInitializer, DedicatedServerModInitializer {
    public static final String MOD_ID = "sgcraftlink";
    public static boolean SHUTTING_DOWN = false;
    private static SGCraftLinkFabric instance;
    private static MinecraftServer server;
    private Set<Module> modules;
    private Logger logger;
    private File dataFolder;
    private Scheduler scheduler;

    @Override
    public void onInitializeServer() {
        instance = this;
        logger = LoggerFactory.getLogger(MOD_ID);

        logger.info("Enabling SGCraftLink!");

        dataFolder = new File("mods" + File.separator + "sgcraftlink" + File.separator);
        dataFolder.mkdirs();

        scheduler = new Scheduler();

        modules = new HashSet<>();
        modules.add(new DataModule(this));
    }

    public void enable() {
        try {
            for (Module module : modules) {
                module.enable();
                logger.info("Enabled {}!", module);

                if (module instanceof ServerPlayerEvents.Join) {
                    ServerPlayerEvents.JOIN.register((ServerPlayerEvents.Join) module);
                }

                if (module instanceof ServerPlayerEvents.Leave) {
                    ServerPlayerEvents.LEAVE.register((ServerPlayerEvents.Leave) module);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disable() {
        logger.info("Disabling SGCraftLink! Goodbye!");

        try {
            for (Module module : modules) {
                module.disable();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        scheduler.shutdown();
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz))
                return clazz.cast(module);
        }

        return null;
    }

    public Set<Module> getModules() {
        return modules;
    }

    public Logger getLogger() {
        return logger;
    }

    public void dispatchCommand(String command) {
        try {
            server.getCommandSource().getDispatcher().execute(command, server.getCommandSource());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onInitialize() {}

    public static SGCraftLinkFabric getInst() {
        return instance;
    }

    public static void setServer(MinecraftServer server) {
        SGCraftLinkFabric.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
