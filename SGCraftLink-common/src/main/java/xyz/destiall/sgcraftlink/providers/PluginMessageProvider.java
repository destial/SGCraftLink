package xyz.destiall.sgcraftlink.providers;

import xyz.destiall.sgcraftlink.SGCraftLinkCommon;

public interface PluginMessageProvider {
    abstract class OutputProvider implements AutoCloseable {
        public abstract void writeUTF(String input);
        public abstract byte[] toByteArray();
        public void close() {}
    }

    OutputProvider getOutputProvider();

    default void jail(String server, int durationInHours, String name, String reason) {
        try (OutputProvider out = getOutputProvider()) {
            out.writeUTF(SGCraftLinkCommon.JAIL);
            out.writeUTF(name.replace("_", "*"));
            out.writeUTF(durationInHours + "h");
            out.writeUTF(reason.replace(" ", "_"));
            if (!sendPluginMessage(server,out.toByteArray())) {
                getPlugin().getLoggerProvider().error("Unable to send " + SGCraftLinkCommon.JAIL + " to " + server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void promoteOrDemote(String promoteDemote, String name, String track) {
        try (OutputProvider out = getOutputProvider()) {
            out.writeUTF(promoteDemote);
            out.writeUTF(name.replace("_", "*"));
            out.writeUTF(track);
            if (!sendPluginMessage(out.toByteArray())) {
                getPlugin().getLoggerProvider().error("Unable to send " + promoteDemote);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void clearInv(String server, String name) {
        try (OutputProvider out = getOutputProvider()) {
            out.writeUTF(SGCraftLinkCommon.CLEARINV);
            out.writeUTF(name.replace("_", "*"));
            if (!sendPluginMessage(server, out.toByteArray())) {
                getPlugin().getLoggerProvider().error("Unable to send " + SGCraftLinkCommon.CLEARINV + " to " + server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void xCatchClear(String server, String name) {
        try (OutputProvider out = getOutputProvider()) {
            out.writeUTF(SGCraftLinkCommon.XCATCHCLEAR);
            out.writeUTF(name.replace("_", "*"));
            if (!sendPluginMessage(server, out.toByteArray())) {
                getPlugin().getLoggerProvider().error("Unable to send " + SGCraftLinkCommon.XCATCHCLEAR + " to " + server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    PluginProvider getPlugin();

    boolean sendPluginMessage(String server, byte[] data);
    boolean sendPluginMessage(byte[] data);
}
