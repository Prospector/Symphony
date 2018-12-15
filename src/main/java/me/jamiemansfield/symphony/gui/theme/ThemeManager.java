package me.jamiemansfield.symphony.gui.theme;

import javafx.scene.Scene;

public class ThemeManager {

    private static Theme currentTheme;

    public static void setTheme(Theme theme, Scene scene) {
        if (currentTheme != null) {
            String oldCss = currentTheme.getCss();
            if (oldCss != null) {
                scene.getStylesheets().remove(oldCss);
            }
        }
        String css = (currentTheme = theme).getCss();
        scene.getStylesheets().add(css);
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

}
