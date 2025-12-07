package course.examples.nt118.model;

import org.json.JSONObject;

public class NotifyEvent {
    private JSONObject data;

    public NotifyEvent(JSONObject data) {
        this.data = data;
    }

    public JSONObject getData() {
        return data;
    }
}