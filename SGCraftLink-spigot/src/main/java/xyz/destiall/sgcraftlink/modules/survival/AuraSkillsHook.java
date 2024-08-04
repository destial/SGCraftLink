package xyz.destiall.sgcraftlink.modules.survival;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class AuraSkillsHook extends Module {
    private final AuraSkillsApi api;
    public AuraSkillsHook(SGCraftLinkSpigot plugin) {
        super(plugin);
        api = AuraSkillsApi.get();
    }

    public double getMana(Player player) {
        SkillsUser user = api.getUser(player.getUniqueId());
        if (user == null)
            return -1;

        return user.getMana();
    }

    public void setMana(Player player, double mana) {
        SkillsUser user = api.getUser(player.getUniqueId());
        if (user == null)
            return;

        user.setMana(mana);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public void enable() {}
}
