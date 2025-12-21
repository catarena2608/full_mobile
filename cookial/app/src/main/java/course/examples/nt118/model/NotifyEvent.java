package course.examples.nt118.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class NotifyEvent {
    // Trường hợp 1: Thông báo lẻ (Realtime)
    private JSONObject jsonData;

    // Trường hợp 2: Danh sách thông báo (Init)
    private JSONArray arrayData;

    // Constructor cho notify lẻ
    public NotifyEvent(JSONObject data) {
        this.jsonData = data;
        this.arrayData = null;
    }

    // Constructor cho list (Init)
    public NotifyEvent(JSONArray data) {
        this.arrayData = data;
        this.jsonData = null;
    }

    public JSONObject getJsonData() {
        return jsonData;
    }

    public JSONArray getArrayData() {
        return arrayData;
    }

    // Helper để biết đây là loại nào
    public boolean isList() {
        return arrayData != null;
    }
}