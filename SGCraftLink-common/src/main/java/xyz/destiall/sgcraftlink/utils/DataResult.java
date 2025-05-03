package xyz.destiall.sgcraftlink.utils;

import org.eclipse.jetty.client.api.ContentResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class DataResult {
    private final int status;
    private final String content;
    private JSONObject object;

    public DataResult(ContentResponse response) {
        this(response.getStatus(), response.getContentAsString().trim());
    }

    public DataResult(int status, String content) {
        this.status = status;
        this.content = content;
        if (isSuccess()) {
            if (content == null || content.isEmpty()) {
                content = "{}";
            }
            try {
                object = new JSONObject(content);
            } catch (JSONException e) {
                e.printStackTrace();
                object = new JSONObject();
            }
        } else {
            object = null;
        }
    }

    public boolean isSuccess() {
        return status == 200;
    }

    public JSONObject getJSON() {
        return object;
    }

    public int getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }
}
