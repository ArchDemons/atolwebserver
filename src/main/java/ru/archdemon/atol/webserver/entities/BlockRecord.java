package ru.archdemon.atol.webserver.entities;

import java.util.Date;
import lombok.Data;

@Data
public class BlockRecord {

    private long id;
    private String deviceId;
    private String data;
    private String uuid;
    private boolean isPaperError;
    private boolean isFnError;
    private boolean isConnectionError;
    private Date createdTime;

}
