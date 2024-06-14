package xyz.destiall.sgcraftlink.modules.hooks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Objects;

public class EconomyHook extends Module {
    private final Economy economy;

    public EconomyHook(SGCraftLinkSpigot plugin) {
        super(plugin);
        economy = Objects.requireNonNull(plugin.getServer().getServicesManager().getRegistration(Economy.class)).getProvider();
    }

    public boolean deposit(OfflinePlayer player, float amount) {
        return economy.depositPlayer(player, amount).type == EconomyResponse.ResponseType.SUCCESS;
    }

    public boolean withdraw(OfflinePlayer player, float amount) {
        return economy.withdrawPlayer(player, amount).type == EconomyResponse.ResponseType.SUCCESS;
    }

    public double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}
}
