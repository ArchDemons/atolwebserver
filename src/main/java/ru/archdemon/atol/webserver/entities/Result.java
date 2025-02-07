package ru.archdemon.atol.webserver.entities;

import java.util.Date;
import lombok.Data;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.archdemon.atol.webserver.Utils;

@Data
public class Result {

    private Date createdTime;
    private int status = 0;
    private int errorCode = 0;
    private String errorDescription = "";
    private String resultData = "";
    private long requestId;
    private Date updatedTime;
    private int number;
    private long id;

    public JSONObject toJson() {
        JSONParser parser = new JSONParser();
        JSONObject subTaskResult = null;
        try {
            subTaskResult = (JSONObject) parser.parse(resultData);
        } catch (ParseException parseException) {
        }

        JSONObject error = new JSONObject();
        error.put("code", errorCode);
        error.put("description", errorDescription);

        JSONObject result = new JSONObject();
        result.put("status", Utils.getStatusString(status));
        result.put("error", error);
        result.put("result", subTaskResult);

        return result;
    }
}
