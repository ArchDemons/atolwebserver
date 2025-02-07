package ru.archdemon.atol.webserver.entities;

import lombok.Data;
import org.json.simple.JSONObject;

@Data
public class Setting {

    private String scriptsPath;
    private boolean invertCdStatus;
    private String headerLines;
    private String footerLines;
    private boolean blockOnPrintErrors;
    private int deleteRequestsAfter;
    private boolean validateRequestsOnAdd;

    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        result.put("scriptsPath", scriptsPath);
        result.put("invertCashDrawerStatus", invertCdStatus);
        result.put("additionalHeaderLines", headerLines);
        result.put("additionalFooterLines", footerLines);
        result.put("blockQueueOnPrintErrors", blockOnPrintErrors);
        result.put("deleteRequestsAfter", deleteRequestsAfter);
        result.put("validateRequestsOnAdd", validateRequestsOnAdd);

        return result;
    }
}
