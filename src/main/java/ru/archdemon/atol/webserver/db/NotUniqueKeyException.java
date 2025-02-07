package ru.archdemon.atol.webserver.db;

public class NotUniqueKeyException extends DBException {

    public NotUniqueKeyException() {
    }

    public NotUniqueKeyException(String message) {
        super(message);
    }

    public NotUniqueKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotUniqueKeyException(Throwable cause) {
        super(cause);
    }
}
