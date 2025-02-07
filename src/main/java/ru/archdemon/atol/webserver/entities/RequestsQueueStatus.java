package ru.archdemon.atol.webserver.entities;

import lombok.Data;

@Data
public class RequestsQueueStatus {

    private int ready;
    private int number;
    private int canceled;
}
