package ru.archdemon.atol.webserver.settings;

public class SelectItem {

    private String id;
    private String name;
    private boolean selected;

    public SelectItem(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isSelected() {
        return this.selected;
    }
}
