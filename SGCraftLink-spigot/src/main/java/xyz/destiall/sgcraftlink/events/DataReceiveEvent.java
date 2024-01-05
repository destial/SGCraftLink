package xyz.destiall.sgcraftlink.events;

import express.utils.Status;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class DataReceiveEvent extends Event {
    public static final HandlerList handlers=new HandlerList();
    @Override public @NotNull HandlerList getHandlers(){return handlers;}
    public static HandlerList getHandlerList(){return handlers;}

    private final JSONObject json;
    private final String dataType;
    private JSONObject response;
    private Status status;

    public DataReceiveEvent(JSONObject json) {
        super(true);
        this.json = json;
        this.dataType = json.getString("data-type");
        this.status = Status._200;
    }

    public JSONObject getJson() {
        return json;
    }

    public void setResponse(JSONObject returning) {
        this.response = returning;
    }

    public JSONObject getResponse() {
        return response;
    }

    public String getDataType() {
        return dataType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
