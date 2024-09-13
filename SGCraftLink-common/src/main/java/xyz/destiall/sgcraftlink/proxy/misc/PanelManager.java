package xyz.destiall.sgcraftlink.proxy.misc;

import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.utils.DataResult;
import xyz.destiall.sgcraftlink.utils.HTTPManager;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PanelManager {
    private final String baseUrl = "https://panel.destial.xyz/api/client/servers/{PANEL_ID}/";
    private final String powerUrl = baseUrl + "power";
    private final String resourcesUrl = baseUrl + "resources";
    private final Map<String, String> headers;

    private final PluginProvider plugin;
    private final HTTPManager hm;
    public PanelManager(PluginProvider plugin, HTTPManager hm) {
        this.plugin = plugin;
        this.hm = hm;
        this.headers = HTTPManager.asMap(
                "Accept", "application/json",
                "Content-Type", "application/json",
                "Authorization", "Bearer ptlc_kg2OtOFBIjxxKDoJNcl4wkcsytGDjnpooizyDhz2mGn"
                // ptlc_kg2OtOFBIjxxKDoJNcl4wkcsytGDjnpooizyDhz2mGn (Client Key)
                // ptla_HHznv0Nzz7VhuuLh15NHEm1b68APa4VAa8AuvgPhjFA (Server Key)
        );

        // Memory checker
        plugin.getScheduler().schedule(() -> {
            plugin.getLoggerProvider().warning("Scheduling memory checker! [" + String.join(",", plugin.getDataManager().getServers()) + "]");
            for (String serverName : plugin.getDataManager().getServers()) {
                RegisteredServer server = plugin.getDataManager().getRegisteredServer(serverName);
                if (server.getPanelId() == null || server.getPanelId().equalsIgnoreCase("null"))
                    continue;

                getResourceUsage(server).whenComplete((resources, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }

                    long memoryBytes = resources.getJSON().getJSONObject("attributes").getJSONObject("resources").getLong("memory_bytes");
                    long maxMemory = server.getMaxMemory();
                    if (maxMemory <= 0)
                        return;

                    double usage = memoryBytes / (double) maxMemory;
                    if (usage >= 0.95d) {
                        plugin.getLoggerProvider().warning(serverName + " is overloading soon! Restarting! (" + (usage * 100d) + "%)");
                        plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, serverName, SGCraftLinkCommon.createCommandsData("cmi schedule stopserver"), (r) -> {});
                    }
                });
            }
        }, 30L, 30L, TimeUnit.SECONDS);
    }

    public CompletableFuture<DataResult> start(RegisteredServer server) {
        return executePower(server, new JSONObject().put("signal", "start"));
    }

    public CompletableFuture<DataResult> stop(RegisteredServer server) {
        return executePower(server, new JSONObject().put("signal", "stop"));
    }

    public CompletableFuture<DataResult> restart(RegisteredServer server) {
        return executePower(server, new JSONObject().put("signal", "restart"));
    }

    public CompletableFuture<DataResult> kill(RegisteredServer server) {
        return executePower(server, new JSONObject().put("signal", "kill"));
    }

    private CompletableFuture<DataResult> executePower(RegisteredServer server, JSONObject signal) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        plugin.getLoggerProvider().warning("Sending " + signal.getString("signal") + " signal to " + server.getPanelId());
        String url = powerUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.POST, headers, signal);
    }

    private CompletableFuture<DataResult> getResourceUsage(RegisteredServer server) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        String url = resourcesUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.GET, headers);
    }

    public CompletableFuture<DataResult> getServerDetails(RegisteredServer server) {
        if (server.getPanelId().equalsIgnoreCase("null"))
            return CompletableFuture.completedFuture(new DataResult(404, null));

        String url = baseUrl.replace("{PANEL_ID}", server.getPanelId());
        return hm.fetch(url, HTTPManager.Method.GET, headers);
    }
}
