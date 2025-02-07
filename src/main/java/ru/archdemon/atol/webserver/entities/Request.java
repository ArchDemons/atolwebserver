package ru.archdemon.atol.webserver.entities;

import java.util.Date;
import lombok.Data;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Data
public class Request {

    private Long id;
    private String uuid;
    private String deviceId;
    private String data;
    private Date createdTime;
    private Date finishedTime;
    private boolean isReady;
    private boolean isCanceled;
    private boolean inProgress;
    private String callbackResultUrl;
    private boolean callbackResultComplete;

    public boolean isList() {
        String trimmed = this.data.trim();
        return (!trimmed.isEmpty() && trimmed.charAt(0) == '[');
    }

    public int getSubTaskCount() {
        JSONParser parser = new JSONParser();
        try {
            Object o = parser.parse(this.data);
            if (o instanceof JSONArray) {
                return ((JSONArray) o).size();
            }
            return 1;
        } catch (ParseException parseException) {

            return 0;
        }
    }

    public JSONArray getDataJson() {
        JSONArray a;
        JSONParser parser = new JSONParser();

        try {
            Object o = parser.parse(this.data);
            if (o instanceof JSONArray) {
                a = (JSONArray) o;
            } else {
                a = new JSONArray();
                a.add((JSONObject) o);
            }
        } catch (ParseException ignored) {
            return null;
        }
        return a;
    }

}
