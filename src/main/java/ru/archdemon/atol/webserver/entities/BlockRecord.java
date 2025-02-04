package ru.archdemon.atol.webserver.entities;

public class BlockRecord {

    private String uuid;
    private long documentNumber;

    public BlockRecord(String uuid, long documentNumber) {
        this.uuid = uuid;
        this.documentNumber = documentNumber;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getDocumentNumber() {
        return this.documentNumber;
    }

    public void setDocumentNumber(long documentNumber) {
        this.documentNumber = documentNumber;
    }
}
