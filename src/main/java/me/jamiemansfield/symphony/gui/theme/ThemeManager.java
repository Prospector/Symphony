package me.jamiemansfield.symphony.gui.theme;

import me.jamiemansfield.symphony.gui.SymphonyMain;

public class ThemeManager {

    private static Theme currentTheme = Theme.NONE;

    public static void setTheme(Theme theme, SymphonyMain symphony) {
        String oldCss = currentTheme.getCss();
        String css = (currentTheme = theme).getCss();
        if (oldCss != null) {
            symphony.getStage().getScene().getStylesheets().remove(oldCss);
        }
        if (theme != Theme.NONE) {
            symphony.getStage().getScene().getStylesheets().add(css);
        }
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

}
