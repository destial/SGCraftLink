package xyz.destiall.sgcraftlink.items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OPDGroup {
    private final List<Pair<UUID, String>> members;
    private final List<Pair<UUID, String>> invites;

    public OPDGroup() {
        this.members = new ArrayList<>();
        this.invites = new ArrayList<>();
    }

    public Pair<UUID, String> getLeader() {
        if (members.isEmpty())
            return null;

        return members.get(0);
    }

    public List<Pair<UUID, String>> getMembers() {
        return members;
    }

    public List<Pair<UUID, String>> getInvites() {
        return invites;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof OPDGroup) {
            OPDGroup other = (OPDGroup) obj;
            return this.getLeader().getKey().equals(other.getLeader().getKey());
        }

        return false;
    }
}
