package me.jamiemansfield.symphony.gui.theme;

import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

public enum Theme {

    NONE("none"),
    MODENA_DARK("modena_dark"),
    SPECTOR_DARK("spector_dark"),
    SPECTOR_LIGHT("spector_light");

    private String name;
    private String css = null;
    private Node packageIcon;
    private Node classIcon;

    Theme(String name) {
        this(name, new FontIcon("fth-folder"), new FontIcon("fth-file"));
    }

    Theme(String name, Node packageIcon, Node classIcon) {
        this.name = name;
        if (!name.equals("none")) {
            this.css = "themes/" + name + "/" + name + ".css";
        }
        this.packageIcon = packageIcon;
        this.classIcon = classIcon;
    }

    public String getName() {
        return name;
    }

    // can return null
    public String getCss() {
        return css;
    }

    public Node getPackageIcon() {
        return packageIcon;
    }

    public Node getClassIcon() {
        return classIcon;
    }
}
