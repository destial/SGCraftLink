package xyz.destiall.sgcraftlink.items;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.INoTargetSkill;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.conditions.ICasterCondition;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.utils.annotations.MythicCondition;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.modules.survival.AuraSkillsHook;

@MythicMechanic(name = "mana", aliases = {"useMana"}, description = "Use mana")
public class AuraSkillsManaMechanic implements INoTargetSkill {
    private final double amount;
    private final AuraSkillsHook hook;
    public AuraSkillsManaMechanic(AuraSkillsHook hook, MythicMechanicLoadEvent loader) {
        this.hook = hook;
        MythicLineConfig mlc = loader.getConfig();
        this.amount = mlc.getDouble(new String[]{"amount", "a"}, 1);
    }

    @Override
    public SkillResult cast(SkillMetadata data) {
        AbstractEntity entity = data.getCaster().getEntity();
        if (!entity.isPlayer()) {
            MythicLogger.debug(MythicLogger.DebugLevel.MECHANIC, "[ConsumeUsedItem] Target is not player");
            return SkillResult.INVALID_TARGET;
        }

        Player player = (Player) entity.getBukkitEntity();
        double mana = hook.getMana(player);
        if (mana < amount) {
            return SkillResult.CONDITION_FAILED;
        }
        hook.setMana(player, mana - amount);
        return SkillResult.SUCCESS;
    }

    @MythicCondition(name = "mana", aliases = {"hasMana"}, description = "Has mana")
    public static class AuraSkillsManaCondition implements ICasterCondition {
        private final double amount;
        private final AuraSkillsHook hook;
        public AuraSkillsManaCondition(AuraSkillsHook hook, MythicConditionLoadEvent loader) {
            this.hook = hook;
            MythicLineConfig mlc = loader.getConfig();
            this.amount = mlc.getDouble(new String[]{"amount", "a"}, 1);
        }
        @Override
        public boolean check(SkillCaster caster) {
            AbstractEntity entity = caster.getEntity();
            if (!entity.isPlayer()) {
                MythicLogger.debug(MythicLogger.DebugLevel.CONDITION, "[ConsumeUsedItem] Target is not player");
                return false;
            }

            Player player = (Player) entity.getBukkitEntity();
            double mana = hook.getMana(player);
            return mana > amount;
        }
    }
}
