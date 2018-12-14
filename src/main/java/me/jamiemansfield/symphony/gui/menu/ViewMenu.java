//******************************************************************************
// Copyright (c) Jamie Mansfield <https://jamiemansfield.me/>
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//******************************************************************************

package me.jamiemansfield.symphony.gui.menu;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import me.jamiemansfield.symphony.gui.theme.Theme;
import me.jamiemansfield.symphony.gui.theme.ThemeManager;
import me.jamiemansfield.symphony.util.LocaleHelper;

/**
 * The Symphony 'View' menu.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public class ViewMenu extends Menu {

	public ViewMenu(final MainMenuBar mainMenuBar) {
		// Settings
		super(LocaleHelper.get("menu.view"));
		this.setMnemonicParsing(true);

		// Themes
		{
			final Menu themes = new Menu(LocaleHelper.get("menu.view.themes"));
			final ToggleGroup themeGroup = new ToggleGroup();
			this.getItems().add(themes);
			for (Theme theme : Theme.values()) {
				RadioMenuItem themeItem = new RadioMenuItem(LocaleHelper.get("theme." + theme.getName()));
				themeItem.setToggleGroup(themeGroup);
				themeItem.addEventHandler(ActionEvent.ACTION, event -> ThemeManager.setTheme(theme, mainMenuBar.getSymphony()));
				if (ThemeManager.getCurrentTheme() == theme) {
					themeItem.setSelected(true);
				}
				themes.getItems().add(themeItem);
			}
		}

		this.getItems().add(new SeparatorMenuItem());

		// Close all tabs
		{
			final MenuItem closeAllTabs = new MenuItem(LocaleHelper.get("menu.view.close_all_tabs"));
			closeAllTabs.addEventHandler(ActionEvent.ACTION, event -> mainMenuBar.getSymphony().getTabs().getTabs().clear());
			this.getItems().add(closeAllTabs);
		}

	}

}
