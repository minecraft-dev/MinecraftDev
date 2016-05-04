package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.platform.Type;
import com.demonwav.mcdev.platform.bukkit.BukkitSettings;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordSettings;
import com.demonwav.mcdev.platform.sponge.SpongeSettings;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.HyperlinkEvent;

public class ProjectChooserWizardStep extends ModuleWizardStep {

    private final MinecraftProjectCreator creator;

    private JPanel chooserPanel;
    private JPanel panel;
    private JPanel infoPanel;

    private JRadioButton bukkitRadioButton;
    private JRadioButton spigotRadioButton;
    private JRadioButton bungeecordRadioButton;
    private JEditorPane infoPane;
    private JRadioButton spongeRadioButton;
    private JRadioButton paperRadioButton;

    private Type type = Type.BUKKIT;

    private static final String bukkitInfo = "<html><font size=\"4\">Create a standard " +
            "<a href=\"http://bukkit.org/\">Bukkit</a> plugin, for use " +
            "on CraftBukkit, Spigot, and Paper servers.</font></html>";
    private static final String spigotInfo = "<html><font size=\"4\">Create a standard " +
            "<a href=\"https://www.spigotmc.org/\">Spigot</a> plugin, for use " +
            "on Spigot and Paper servers.</font></html>";
    private static final String paperInfo = "<html><font size=\"4\">Create a standard " +
            "<a href=\"https://paper.readthedocs.io/en/paper-1.9/\">Paper</a> plugin, for use " +
            "on Paper servers.</font></html>";
    private static final String bungeeCordInfo = "<html><font size=\"4\">Create a standard " +
            "<a href=\"https://www.spigotmc.org/wiki/bungeecord/\"> BungeeCord</a> plugin, for use " +
            "on BungeeCord servers.</font></html>";
    private static final String spongeInfo = "<html><font size=\"4\">Create a standard " +
            "<a href=\"https://www.spongepowered.org/\"> Sponge</a> plugin, for use " +
            "on Sponge servers.</font></html>";

    public ProjectChooserWizardStep(@NotNull MinecraftProjectCreator creator) {
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
                creator.setSettings(new BukkitSettings());
            }
        });
        spigotRadioButton.addChangeListener(e -> {
            if (type != Type.SPIGOT) {
                type = Type.SPIGOT;
                infoPane.setText(spigotInfo);
                creator.setType(type);
                creator.setSettings(new BukkitSettings());
            }
        });
        paperRadioButton.addChangeListener(e -> {
            if (type != Type.PAPER) {
                type = Type.PAPER;
                infoPane.setText(paperInfo);
                creator.setType(type);
                creator.setSettings(new BukkitSettings());
            }
        });

        spongeRadioButton.addChangeListener(e -> {
            if (type != Type.SPONGE) {
                type = Type.SPONGE;
                infoPane.setText(spongeInfo);
                creator.setType(type);
                creator.setSettings(new SpongeSettings());
            }
        });
        bungeecordRadioButton.addChangeListener(e -> {
            if (type != Type.BUNGEECORD) {
                type = Type.BUNGEECORD;
                infoPane.setText(bungeeCordInfo);
                creator.setType(type);
                creator.setSettings(new BungeeCordSettings());
            }
        });
        return panel;
    }

    @Override
    public void updateDataModel() {}
}
