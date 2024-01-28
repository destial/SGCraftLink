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
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        plugin.getLoggerProvider().warning("Sending start command to " + server.getPanelId());
        String url = baseUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.POST, headers, new JSONObject().put("signal", "start"));
    }

    public CompletableFuture<DataResult> stop(RegisteredServer server) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        plugin.getLoggerProvider().warning("Sending stop command to " + server.getPanelId());
        String url = baseUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.POST, headers, new JSONObject().put("signal", "stop"));
    }

    public CompletableFuture<DataResult> restart(RegisteredServer server) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        plugin.getLoggerProvider().warning("Sending restart command to " + server.getPanelId());
        String url = baseUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.POST, headers, new JSONObject().put("signal", "restart"));
    }

    public CompletableFuture<DataResult> kill(RegisteredServer server) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        plugin.getLoggerProvider().warning("Sending kill command to " + server.getPanelId());
        String url = baseUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.POST, headers, new JSONObject().put("signal", "kill"));
    }
}
