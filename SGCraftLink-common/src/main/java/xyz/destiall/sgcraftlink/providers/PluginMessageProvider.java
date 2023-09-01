package xyz.destiall.sgcraftlink.providers;

public interface PluginMessageProvider {
    abstract class OutputProvider implements AutoCloseable {
        public abstract void writeUTF(String input);
        public abstract byte[] toByteArray();
        public void close() {}
    }

    OutputProvider getOutputProvider();

    PluginProvider getPlugin();

    boolean sendPluginMessage(String server, byte[] data);
    boolean sendPluginMessage(byte[] data);
}
