package ru.archdemon.atol.webserver.entities;

import java.util.Date;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Task {

    private String uuid;
    private String data;
    private Date timestamp;
    private boolean isReady;
    private boolean isCanceled;

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isList() {
        String trimmed = this.data.trim();
        return (!trimmed.isEmpty() && trimmed.charAt(0) == '[');
    }

    public boolean isReady() {
        return this.isReady;
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
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

    public boolean isCanceled() {
        return this.isCanceled;
    }

    public void setCanceled(boolean canceled) {
        this.isCanceled = canceled;
    }
}
