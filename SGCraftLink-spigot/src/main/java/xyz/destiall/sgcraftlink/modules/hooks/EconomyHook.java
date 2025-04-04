package xyz.destiall.sgcraftlink.modules.hooks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class EconomyHook extends Module {
    private Economy economy;

    public EconomyHook(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @Override
    public void initConfig() {
        RegisteredServiceProvider<Economy> provider = plugin
                .getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (provider != null)
            economy = provider.getProvider();
        super.initConfig();
    }

    public boolean deposit(OfflinePlayer player, float amount) {
        if (!isEnabled())
            return false;
        return economy.depositPlayer(player, amount).type == EconomyResponse.ResponseType.SUCCESS;
    }

    public boolean withdraw(OfflinePlayer player, float amount) {
        if (!isEnabled())
            return false;
        return economy.withdrawPlayer(player, amount).type == EconomyResponse.ResponseType.SUCCESS;
    }

    public double balance(OfflinePlayer player) {
        if (!isEnabled())
            return -1d;
        return economy.getBalance(player);
    }

    @Override
    public boolean isEnabled() {
        return economy != null;
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public void enable() {}

    @Override
    public boolean isPersistent() {
        return true;
    }
}
