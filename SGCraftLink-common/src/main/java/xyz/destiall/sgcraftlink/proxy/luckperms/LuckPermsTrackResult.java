package xyz.destiall.sgcraftlink.proxy.luckperms;

import net.luckperms.api.model.user.User;

public class LuckPermsTrackResult {
    public final User user;
    public final String group;

    public LuckPermsTrackResult(User user, String track) {
        this.user = user;
        this.group = track;
    }
}
