/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.creator;

import com.demonwav.bukkitplugin.BukkitProject.Type;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.HyperlinkEvent;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;

public class BukkitWizardStep extends ModuleWizardStep {

    private final MavenProjectCreator creator;

    private JPanel chooserPanel;
    private JPanel panel;
    private JPanel infoPanel;

    private JRadioButton bukkitRadioButton;
    private JRadioButton spigotRadioButton;
    private JRadioButton bungeeCordRadioButton;
    private JEditorPane infoPane;

    private Type type = Type.BUKKIT;

    private static final String bukkitInfo = "<html><font size=\"4\">Create a standard " +
        "<a href=\"https://maven.apache.org/\">Maven</a> <a href=\"http://bukkit.org/\">Bukkit</a> plugin, for use " +
        "on CraftBukkit and Spigot servers.<br>Generates a default <code>pom.xml</code> and <code>plugin.yml</code>, " +
        "and creates a default run configuration to build.</font></html>";
    private static final String spigotInfo = "<html><font size=\"4\">Create a standard " +
        "<a href=\"https://maven.apache.org/\">Maven</a> <a href=\"https://www.spigotmc.org/\">Spigot</a> plugin," +
        " for use on Spigot servers.<br>Generates a default <code>pom.xml</code> and <code>plugin.yml</code>, and " +
        "creates a default run configuration to build.</font></html>";
    private static final String bungeeCordInfo = "<html><font size=\"4\">Create a standard " +
        "<a href=\"https://maven.apache.org/\">Maven</a> <a href=\"https://www.spigotmc.org/wiki/bungeecord/\">" +
        "BungeeCord</a> plugin, for use on BungeeCord servers.<br>Generates a default <code>pom.xml</code> and " +
        "<code>plugin.yml</code>, and creates a default run configuration to build.</font></html>";

    public BukkitWizardStep(@NotNull MavenProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        chooserPanel.setBorder(IdeBorderFactory.createBorder());
        infoPanel.setBorder(IdeBorderFactory.createBorder());

        // HTML parsing and hyperlink support
        infoPane.setContentType("text/html");
        infoPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (URISyntaxException | IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        // Set initial text
        infoPane.setText(bukkitInfo);

        // Set type
        bukkitRadioButton.addChangeListener(e -> {
            if (type != Type.BUKKIT) {
                type = Type.BUKKIT;
                infoPane.setText(bukkitInfo);
                creator.setType(type);
            }
        });
        spigotRadioButton.addChangeListener(e -> {
            if (type != Type.SPIGOT) {
                type = Type.SPIGOT;
                infoPane.setText(spigotInfo);
                creator.setType(type);
            }
        });
        bungeeCordRadioButton.addChangeListener(e -> {
            if (type != Type.BUNGEECORD) {
                type = Type.BUNGEECORD;
                infoPane.setText(bungeeCordInfo);
                creator.setType(type);
            }
        });

        return panel;
    }

    @Override
    public void updateDataModel() {}
}
