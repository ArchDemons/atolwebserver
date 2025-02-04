package ru.archdemon.atol.webserver;

public class Version {

    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
}
