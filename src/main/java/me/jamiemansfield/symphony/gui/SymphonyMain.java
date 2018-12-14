//******************************************************************************
// Copyright (c) Jamie Mansfield <https://jamiemansfield.me/>
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//******************************************************************************

package me.jamiemansfield.symphony.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.jamiemansfield.symphony.SharedConstants;
import me.jamiemansfield.symphony.gui.menu.MainMenuBar;
import me.jamiemansfield.symphony.gui.tab.code.CodeTab;
import me.jamiemansfield.symphony.gui.tab.welcome.WelcomeTab;
import me.jamiemansfield.symphony.gui.theme.Theme;
import me.jamiemansfield.symphony.gui.theme.ThemeManager;
import me.jamiemansfield.symphony.gui.tree.*;
import me.jamiemansfield.symphony.jar.Jar;
import me.jamiemansfield.symphony.util.StateHelper;
import org.cadixdev.bombe.jar.JarClassEntry;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

import java.io.IOException;
import java.util.*;

/**
 * The Main-Class behind Symphony.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public final class SymphonyMain extends Application {

	private Stage stage;
	private TabPane tabs;

	// Active jar
	private Jar jar;

	// Classes View
	private TreeItem<TreeElement> treeRoot;

	@Override
	public void start(final Stage primaryStage) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (this.jar == null) return;
			try {
				this.jar.close();
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}));

		// Set the primary stage
		this.stage = primaryStage;
		this.stage.setTitle("Symphony v" + SharedConstants.VERSION);
		this.stage.setWidth(1280);
		this.stage.setHeight(720);

		// Root GUI container
		final BorderPane root = new BorderPane();

		// Main Menu
		final MainMenuBar mainMenu = new MainMenuBar(this);
		root.setTop(mainMenu);

		// Main Section
		final SplitPane main = new SplitPane();

		// Classes view
		final BorderPane classesView = new BorderPane();
		classesView.setMaxWidth(400);
		classesView.setMinWidth(250);
		// TODO: Search
		{
			final TreeView<TreeElement> treeView = new TreeView<>();
			treeView.setShowRoot(false);
			treeView.setCellFactory(view -> new SymphonyTreeCell());
			treeView.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2) {
					final TreeItem<TreeElement> item = treeView.getSelectionModel().getSelectedItems().get(0);
					if (item == null) return;
					item.getValue().activate();
				}
			});
			this.treeRoot = new TreeItem<>(new RootElement());
			this.treeRoot.setExpanded(true);
			treeView.setRoot(this.treeRoot);

			final ScrollPane scrollPane = new ScrollPane(treeView);
			scrollPane.setFitToWidth(true);
			scrollPane.setFitToHeight(true);

			classesView.setCenter(scrollPane);
		}
		main.getItems().add(classesView);

		// Tabs
		this.tabs = new TabPane();
		{
			this.tabs.getTabs().add(new WelcomeTab());
		}
		main.getItems().add(this.tabs);

		main.setDividerPositions(0.1);
		root.setCenter(main);

		// Set the scene
		final Scene scene = new Scene(root);
		scene.getStylesheets().add("css/highlighting.css");
		ThemeManager.setTheme(Theme.NONE, this);
		this.stage.setScene(scene);
		this.stage.show();
	}

	public Jar getJar() {
		return this.jar;
	}

	public void setJar(final Jar jar) {
		this.jar = jar;
	}

	public TabPane getTabs() {
		return this.tabs;
	}

	public Stage getStage() {
		return this.stage;
	}

	public void refreshClasses() {
		final List<String> expanded = this.getExpandedPackages(new ArrayList<>(), this.treeRoot);
		this.treeRoot.getChildren().clear();
		if (this.jar == null) return;

		final Map<String, TreeItem<TreeElement>> packageCache = new HashMap<>();

		this.jar.entries()
			.filter(JarClassEntry.class::isInstance).map(JarClassEntry.class::cast)
			.filter(entry -> !entry.getSimpleName().contains("$"))
			.forEach(entry -> {
				final String klassName = entry.getName().substring(0, entry.getName().length() - ".class".length());
				final TopLevelClassMapping klass = this.jar.getMappings().getOrCreateTopLevelClassMapping(klassName);

				this.getPackageItem(packageCache, klass.getDeobfuscatedPackage()).getChildren()
					.add(new TreeItem<>(new ClassElement(this, klass)));
			});

		// sort
		packageCache.values().forEach(item -> {
			item.getChildren().setAll(item.getChildren().sorted(Comparator.comparing(TreeItem::getValue)));
		});
		this.treeRoot.getChildren().setAll(this.treeRoot.getChildren().sorted(Comparator.comparing(TreeItem::getValue)));

		// reopen packages
		expanded.forEach(pkg -> {
			final TreeItem<TreeElement> packageItem = packageCache.get(pkg);
			if (packageItem == null) return;
			packageItem.setExpanded(true);
		});
	}

	private TreeItem<TreeElement> getPackageItem(final Map<String, TreeItem<TreeElement>> cache, final String packageName) {
		if (packageName.isEmpty()) return this.treeRoot;
		if (cache.containsKey(packageName)) return cache.get(packageName);

		final TreeItem<TreeElement> parent;
		if (packageName.lastIndexOf('/') != -1) {
			parent = this.getPackageItem(cache, packageName.substring(0, packageName.lastIndexOf('/')));
		} else {
			parent = this.treeRoot;
		}
		final TreeItem<TreeElement> packageItem = new TreeItem<>(new PackageElement(this, packageName));
		parent.getChildren().add(packageItem);
		cache.put(packageName, packageItem);
		return packageItem;
	}

	private List<String> getExpandedPackages(final List<String> packages, final TreeItem<TreeElement> item) {
		item.getChildren().filtered(TreeItem::isExpanded).forEach(pkg -> {
			this.getExpandedPackages(packages, pkg);
			if (pkg.getValue() instanceof PackageElement) {
				packages.add(((PackageElement) pkg.getValue()).getName());
			}
		});
		return packages;
	}

	public void update() {
		this.refreshClasses();
		this.tabs.getTabs().stream().filter(CodeTab.class::isInstance).map(CodeTab.class::cast)
			.forEach(CodeTab::update);
	}

	public void displayCodeTab(final TopLevelClassMapping klass) {
		this.tabs.getSelectionModel().select(this.tabs.getTabs().stream()
			.filter(CodeTab.class::isInstance)
			.map(CodeTab.class::cast)
			.filter(tab -> Objects.equals(tab.getKlass(), klass))
			.findFirst()
			.orElseGet(() -> {
				final CodeTab tab = new CodeTab(this, klass);
				this.tabs.getTabs().add(tab);
				return tab;
			}));
	}

	public static void main(final String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(StateHelper::save));

		launch(args);
	}

}
