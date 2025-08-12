package xyz.destiall.sgcraftlink.utils;

import org.eclipse.jetty.client.util.BytesContentProvider;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class JSONContentProvider extends BytesContentProvider {
    public JSONContentProvider(JSONObject object) {
        super("application/json", new byte[][]{object.toString().getBytes(StandardCharsets.UTF_8)});
    }
}
