package xyz.destiall.sgcraftlink;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SGCraftLinkFabric implements ModInitializer, DedicatedServerModInitializer {
    public static final String MOD_ID = "sgcraftlink";
    private static SGCraftLinkFabric instance;
    private static MinecraftServer server;
    private Set<Module> modules;
    private Logger logger;
    private File dataFolder;

    @Override
    public void onInitializeServer() {
        instance = this;
        logger = LoggerFactory.getLogger(MOD_ID);
        dataFolder = new File("sgcraftlink" + File.pathSeparator);

        modules = new HashSet<>();
        modules.add(new DataModule(this));

        for (Module module : modules) {
            module.enable();
        }
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

    @Override
    public void onInitialize() {}

    public static SGCraftLinkFabric getInst() {
        return instance;
    }

    public static void setServer(MinecraftServer server) {
        SGCraftLinkFabric.server = server;
    }
}
