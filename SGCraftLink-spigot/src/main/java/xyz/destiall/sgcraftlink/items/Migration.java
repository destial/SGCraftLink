package xyz.destiall.sgcraftlink.items;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.spawnchunk.piggybank.modules.Account;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.common.MigrationModule;
import xyz.destiall.sgcraftlink.modules.vanilla.ReputationModule;
import xyz.destiall.survivalplots.SurvivalPlotsPlugin;
import xyz.destiall.survivalplots.plot.SurvivalPlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Migration {
    private final MigrationModule module;

    private final String oldName;
    private final String newName;
    private boolean migrated = false;

    public Migration(MigrationModule module, String oldName, String newName) {
        this.module = module;
        this.oldName = oldName;
        this.newName = newName;
    }

    public void setMigrated(boolean migrated) {
        this.migrated = migrated;
    }

    public boolean hasMigrated() {
        return migrated;
    }

    public boolean migrate(boolean remote) {
        if (hasMigrated())
            return true;

        OfflinePlayer source = module.getPlugin().getServer().getOfflinePlayer(oldName);
        Player target = module.getPlugin().getServer().getPlayer(newName);
        if (source.getName() == null || target == null) {
            module.getPlugin().getLogger().warning("Both players must be cached!");
            return false;
        }

        for (Statistic statistic : Statistic.values()) {
            switch (statistic) {
                case MINE_BLOCK: {
                    for (Material material : Material.values()) {
                        if (!material.isBlock())
                            continue;

                        int i = source.getStatistic(statistic, material);
                        if (i == 0)
                            continue;

                        target.setStatistic(statistic, material, i);
                    }

                    continue;
                }
                case BREAK_ITEM:
                case USE_ITEM: {
                    for (Material material : Material.values()) {
                        if (!material.isItem())
                            continue;

                        int i = source.getStatistic(statistic, material);
                        if (i == 0)
                            continue;

                        target.setStatistic(statistic, material, i);
                    }

                    continue;
                }
                case PICKUP:
                case DROP:
                case CRAFT_ITEM: {
                    for (Material material : Material.values()) {
                        int i = source.getStatistic(statistic, material);
                        if (i == 0)
                            continue;

                        target.setStatistic(statistic, material, i);
                    }

                    continue;
                }
                case KILL_ENTITY:
                case ENTITY_KILLED_BY: {
                    for (EntityType entityType : EntityType.values()) {
                        if (entityType == EntityType.UNKNOWN)
                            continue;

                        int i = source.getStatistic(statistic, entityType);
                        if (i == 0)
                            continue;

                        target.setStatistic(statistic, entityType, i);
                    }

                    continue;
                }

                default: break;
            }

            int i = source.getStatistic(statistic);
            if (i == 0)
                continue;

            target.setStatistic(statistic, i);
        }

        if (module.hasCmi()) {
            CMIUser cmiSource = CMI.getInstance().getPlayerManager().getUser(source);
            CMIUser cmiTarget = CMI.getInstance().getPlayerManager().getUser(target);
            if (cmiSource == null || cmiTarget == null) {
                module.getPlugin().getLogger().warning("Unable to load source and target user in CMI!");
                return false;
            }

            cmiTarget.setVotifierVotes(cmiSource.getVotifierVotes());
            if (!CMI.getInstance().getPlayerManager().switchPlayerData(cmiSource, cmiTarget)) {
                module.getPlugin().getLogger().warning("Unable to switchPlayerData in CMI!");
                return false;
            }

            module.getPlugin().getLogger().info("Migrated CMI");
        }

        if (module.hasPiggybank()) {
            double sourceBalance = Account.readBalance(source);
            Account.updateBalance(target, sourceBalance);
            Account.updateBalance(source, 0);

            module.getPlugin().getLogger().info("Migrated PiggyBank");
        }

        if (module.hasLuckperms() && !remote) {
            User lpSource = LuckPermsProvider.get().getUserManager().loadUser(source.getUniqueId()).join();
            User lpTarget = LuckPermsProvider.get().getUserManager().getUser(target.getUniqueId());
            if (lpSource == null || lpTarget == null) {
                module.getPlugin().getLogger().warning("Unable to load source and target user in LuckPerms!");
                return false;
            }

            NodeMap nodeMap = lpTarget.data();
            for (Node node : lpSource.getNodes()) {
                nodeMap.add(node.toBuilder().build());
            }

            LuckPermsProvider.get().getUserManager().saveUser(lpTarget).whenComplete((u, e) -> {
                LuckPermsProvider.get().getMessagingService().get().pushUserUpdate(lpTarget);
            });

            lpSource.data().clear();
            LuckPermsProvider.get().getUserManager().saveUser(lpSource).whenComplete((u, e) -> {
                LuckPermsProvider.get().getMessagingService().get().pushUserUpdate(lpSource);
            });

            module.getPlugin().getLogger().info("Migrated Permissions");
        }

        if (module.hasParties() && !remote) {
            Party partySource = Parties.getApi().getPartyOfPlayer(source.getUniqueId());
            if (partySource != null) {
                PartyPlayer partyPlayerTarget = Parties.getApi().getPartyPlayer(target.getUniqueId());
                if (partyPlayerTarget == null) {
                    module.getPlugin().getLogger().warning("Unable to get target party player in Parties!");
                } else if (!partySource.addMember(partyPlayerTarget)) {
                    module.getPlugin().getLogger().warning("Unable to add target to source party in Parties!");
                }

                PartyPlayer sourcePlayer = Parties.getApi().getPartyPlayer(source.getUniqueId());
                if (sourcePlayer != null) {
                    partySource.removeMember(sourcePlayer);
                }

                module.getPlugin().getLogger().info("Migrated Parties");
            }
        }

        if (module.hasGriefPrevention()) {
            PlayerData sourceData = GriefPrevention.instance.dataStore.getPlayerData(source.getUniqueId());
            List<Claim> sourceClaims = new ArrayList<>(sourceData.getClaims());
            for (Claim sourceClaim : sourceClaims) {
                GriefPrevention.instance.dataStore.changeClaimOwner(sourceClaim, target.getUniqueId());
            }
            module.getPlugin().getLogger().info("Migrated GriefPrevention");
        }

        if (module.hasSurvivalPlots()) {
            List<SurvivalPlot> sourcePlots = SurvivalPlotsPlugin.getInst().getPlotManager().getOwnedPlots(oldName);
            for (SurvivalPlot sourcePlot : sourcePlots) {
                sourcePlot.setOwner(newName);
                sourcePlot.updateExpiry();
            }

            SurvivalPlotsPlugin.getInst().getPlotManager().update();
            module.getPlugin().getLogger().info("Migrated SurvivalPlots");
        }

        ReputationModule reputation = module.getPlugin().getModule(ReputationModule.class);
        if (reputation != null) {
            PlayerReputation sourceReputation = reputation.getReputation(oldName);
            PlayerReputation targetReputation = reputation.getReputation(newName);
            targetReputation.setDayJoinStreak(sourceReputation.getDayJoinStreak());
            targetReputation.setLastJoinDate(sourceReputation.getLastJoinDate());
            targetReputation.setPlayerRatings(sourceReputation.getPlayerRatings());
            targetReputation.setTotalRatings(sourceReputation.getTotalRatings());
            targetReputation.setLongestDayJoinStreak(sourceReputation.getLongestDayJoinStreak());

            reputation.updateReputation(targetReputation);

            module.getPlugin().getLogger().info("Migrated Reputation");
        }

        DataModule dataModule = module.getPlugin().getModule(DataModule.class);
        if (dataModule != null) {
            dataModule.sendProxyData(SGCraftLinkCommon.DATA_PLAYER_MIGRATE, SGCraftLinkCommon.createDataMap("old_name", oldName, "new_name", newName), (r) -> {});
        }

        module.getPlugin().getLogger().info("Saved migrated data!");

        setMigrated(true);
        return true;
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Migration migration = (Migration) o;
        return Objects.equals(oldName, migration.oldName);
    }
}
