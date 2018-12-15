package me.jamiemansfield.symphony.gui.theme;

public enum Theme {

    NONE("none", "fth-folder", "fth-file"),
    SPECTOR_DARK("spector_dark", "eli-folder", "fth-file"),
    SPECTOR_LIGHT("spector_light", "eli-folder", "fth-file");

    private String name;
    private String css;

    Theme(String name, String packageIcon, String classIcon) {
        this.name = name;
        this.css = "themes/" + name + "/" + name + ".css";
    }

    public String getName() {
        return name;
    }

    public String getCss() {
        return css;
    }
}
