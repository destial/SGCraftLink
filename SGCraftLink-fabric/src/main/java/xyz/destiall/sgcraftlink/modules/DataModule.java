package xyz.destiall.sgcraftlink.modules;

import express.Express;
import express.utils.Status;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkFabric;

import java.security.SecureRandom;
import java.util.Base64;

public class DataModule extends Module {
    private final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe
    private final Express express;

    public DataModule(SGCraftLinkFabric mod) {
        super(mod);
        express = new Express();
    }

    @Override
    public void enable() {
        express.post("/", (request, response) -> {
            try {
                String body = SGCraftLinkCommon.convertStreamToString(request.getBody()).trim();
                warn("Received data " + body);
                JSONObject json = new JSONObject(body);

                if (!json.has("data-type")) {
                    warn("Request does not have any data-type!");
                    response.sendStatus(Status._500);
                    return;
                }

                String datatype = json.getString("data-type").toUpperCase();

                if (!datatype.equalsIgnoreCase(SGCraftLinkCommon.DATA_SERVER_REGISTER)) {
                    String auth = config.getString("authentication-key");
                    if (!request.getHeader("X-Authorization").contains(auth)) {
                        response.sendStatus(Status._403);
                        return;
                    }
                }

                switch (datatype) {
                    case SGCraftLinkCommon.DATA_EXECUTE_COMMAND: {
                        JSONArray commands = json.optJSONArray("commands");
                        if (commands != null) {
                            for (int i = 0; i < commands.length(); i++) {
                                String command = commands.getString(i);
                                mod.dispatchCommand(command);
                            }
                        } else {
                            String cmd = json.optString("commands");
                            mod.dispatchCommand(cmd);
                        }
                        response.sendStatus(Status._200);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendStatus(Status._201);
            }
        });
        express.listen(config.getInt("port"));
    }

    public void loadDefaultConfig() {
        config.put("enabled", true);
        config.put("server-name", "new server");
        config.put("port", 9999);
        config.put("proxy-port", 1035);
        config.put("proxy-ip", "172.18.0.1");
        config.put("announce", false);
        config.put("authentication-key", generateNewToken());
        config.put("check-proxy", false);
    }

    public String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    @Override
    public void disable() {
        express.stop();
    }
}
