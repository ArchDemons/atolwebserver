package ru.archdemon.atol.webserver.entities;

import java.util.Date;

public class SubtaskStatus {

    private Date timestamp;
    private int status = 0;
    private int errorCode = 0;
    private String errorDescription = "";
    private String resultData = "";

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return this.errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getResultData() {
        return this.resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }
}
