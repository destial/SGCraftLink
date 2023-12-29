package xyz.destiall.sgcraftlink.items;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.spawnchunk.piggybank.modules.Account;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.modules.MigrationModule;

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
            return false;

        OfflinePlayer source = module.getPlugin().getServer().getOfflinePlayer(oldName);
        Player target = module.getPlugin().getServer().getPlayer(newName);
        if (source.getName() == null || target == null) {
            module.getPlugin().getLogger().warning("Both players must be cached!");
            return false;
        }

        for (Statistic statistic : Statistic.values()) {
            try {
                target.setStatistic(statistic, source.getStatistic(statistic));
            } catch (IllegalArgumentException e1) {
                try {
                    for (EntityType entityType : EntityType.values()) {
                        target.setStatistic(statistic, source.getStatistic(statistic, entityType));
                    }
                } catch (IllegalArgumentException e2) {
                    try {
                        for (Material material : Material.values()) {
                            target.setStatistic(statistic, source.getStatistic(statistic, material));
                        }
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
            }
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
        }

        if (module.hasPiggybank()) {
            double sourceBalance = Account.readBalance(source);
            Account.updateBalance(target, sourceBalance);
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
        }

        if (module.hasParties() && !remote) {
            Party partySource = Parties.getApi().getPartyOfPlayer(source.getUniqueId());
            if (partySource != null) {
                PartyPlayer partyPlayerTarget = Parties.getApi().getPartyPlayer(target.getUniqueId());
                if (partyPlayerTarget == null) {
                    module.getPlugin().getLogger().warning("Unable to get target party player in Parties!");
                    return false;
                }
                if (!partySource.addMember(partyPlayerTarget)) {
                    module.getPlugin().getLogger().warning("Unable to add target to source party in Parties!");
                    return false;
                }
            }
        }

        target.saveData();

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
