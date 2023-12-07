package xyz.destiall.sgcraftlink.providers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public abstract class PlayerProvider {
    protected User luckpermsUser;
    public abstract void sendData(String channel, byte[] data);

    public abstract String getName();

    public abstract UUID getUUID();

    public abstract String getServer();

    public abstract boolean hasPermission(String perms);

    public abstract void sendMessage(String msg);

    public abstract void sendMessageComponent(ComponentProvider<?>... componentProviders);

    public boolean isOnline() {
        return true;
    }

    public void sendPluginMessage(String... data) {
        if (!isOnline())
            return;

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF(data[0]);
            out.writeUTF(getName());
            if (data.length > 1) {
                for (int i = 1; i < data.length; i++) {
                    out.writeUTF(data[i]);
                }
            }
            sendData(SGCraftLinkCommon.CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final User getLuckPermsUser() {
        if (getUUID() == null)
            return null;

        LuckPerms api = LuckPermsProvider.get();
        UserManager manager = api.getUserManager();
        if (luckpermsUser != null && manager.isLoaded(getUUID())) {
            return luckpermsUser;
        }

        luckpermsUser = manager.getUser(getUUID());
        if (luckpermsUser == null) {
            try {
                luckpermsUser = manager.loadUser(getUUID()).get();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return luckpermsUser;
    }

    public String getPrefix() {
        User user = getLuckPermsUser();
        return user == null ? null : user.getCachedData().getMetaData().getPrefix();
    }

    public String getSuffix() {
        User user = getLuckPermsUser();
        return user == null ? null : user.getCachedData().getMetaData().getSuffix();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (getClass().equals(obj.getClass())) {
            PlayerProvider provider = (PlayerProvider) obj;
            UUID uuid = provider.getUUID();
            return uuid == null ? getUUID() == null : uuid.equals(getUUID());
        }

        return false;
    }
}
