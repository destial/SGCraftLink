package xyz.destiall.sgcraftlink.proxy.misc;

import org.json.JSONObject;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.utils.DataResult;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PanelManager {
    private final String baseUrl = "https://panel.destial.xyz/api/client/servers/{PANEL_ID}/power";
    private final Map<String, String> headers;

    private final PluginProvider plugin;
    private final HTTPManager hm;
    public PanelManager(PluginProvider plugin, HTTPManager hm) {
        this.plugin = plugin;
        this.hm = hm;
        this.headers = HTTPManager.asMap(
                "Accept", "application/json",
                "Content-Type", "application/json",
                "Authorization", "Bearer ptlc_rrNIJsNr0tDLqiSTr6htOVaYEhwzLnpYfcZjj2oeJlV"
        );
    }

    public CompletableFuture<DataResult> start(RegisteredServer server) {
        return execute(server, new JSONObject().put("signal", "start"));
    }

    public CompletableFuture<DataResult> stop(RegisteredServer server) {
        return execute(server, new JSONObject().put("signal", "stop"));
    }

    public CompletableFuture<DataResult> restart(RegisteredServer server) {
        return execute(server, new JSONObject().put("signal", "restart"));
    }

    public CompletableFuture<DataResult> kill(RegisteredServer server) {
        return execute(server, new JSONObject().put("signal", "kill"));
    }

    private CompletableFuture<DataResult> execute(RegisteredServer server, JSONObject signal) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        plugin.getLoggerProvider().warning("Sending " + signal.getString("signal") + " command to " + server.getPanelId());
        String url = baseUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.POST, headers, signal);
    }
}
