package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

public class ProjectChooserWizardStep extends ModuleWizardStep {

    private final MinecraftProjectCreator creator;

    private JPanel chooserPanel;
    private JPanel panel;
    private JPanel infoPanel;

    private JEditorPane infoPane;
    private JLabel spongeIcon;
    private JCheckBox bukkitPluginCheckBox;
    private JCheckBox spigotPluginCheckBox;
    private JCheckBox paperPluginCheckBox;
    private JCheckBox spongePluginCheckBox;
    private JCheckBox forgeModCheckBox;
    private JCheckBox bungeeCordPluginCheckBox;
    private JCheckBox liteLoaderModCheckBox;

    @NotNull private static final String bukkitInfo = "Create a standard " +
            "<a href=\"http://bukkit.org/\">Bukkit</a> plugin, for use " +
            "on CraftBukkit, Spigot, and Paper servers.";
    @NotNull private static final String spigotInfo = "Create a standard " +
            "<a href=\"https://www.spigotmc.org/\">Spigot</a> plugin, for use " +
            "on Spigot and Paper servers.";
    @NotNull private static final String paperInfo = "Create a standard " +
            "<a href=\"https://paper.emc.gs\">Paper</a> plugin, for use " +
            "on Paper servers.";
    @NotNull private static final String bungeeCordInfo = "Create a standard " +
            "<a href=\"https://www.spigotmc.org/wiki/bungeecord/\"> BungeeCord</a> plugin, for use " +
            "on BungeeCord servers.";
    @NotNull private static final String spongeInfo = "Create a standard " +
            "<a href=\"https://www.spongepowered.org/\"> Sponge</a> plugin, for use " +
            "on Sponge servers.";
    @NotNull private static final String forgeInfo = "Create a standard " +
            "<a href=\"http://files.minecraftforge.net/\"> Forge</a> mod, for use " +
            "on Forge servers and clients.";
    @NotNull private static final String liteLoaderInfo = "Create a standard " +
            "<a href=\"http://www.liteloader.com/\"> LiteLoader</a> mod, for use " +
            "on LiteLoader clients.";

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

        // Set types
        bukkitPluginCheckBox.addActionListener(e ->
                toggle(bukkitPluginCheckBox, spigotPluginCheckBox, paperPluginCheckBox)
        );

        spigotPluginCheckBox.addActionListener(e ->
                toggle(spigotPluginCheckBox, bukkitPluginCheckBox, paperPluginCheckBox)
        );

        paperPluginCheckBox.addActionListener(e ->
                toggle(paperPluginCheckBox, bukkitPluginCheckBox, spigotPluginCheckBox)
        );

        spongePluginCheckBox.addActionListener(e ->
                fillInInfoPane()
        );

        forgeModCheckBox.addActionListener(e ->
                fillInInfoPane()
        );

        liteLoaderModCheckBox.addActionListener(e ->
                fillInInfoPane()
        );

        bungeeCordPluginCheckBox.addActionListener(e ->
                fillInInfoPane()
        );

        spongeIcon.setIcon(PlatformAssets.SPONGE_ICON_2X);

        return panel;
    }

    private void toggle(JCheckBox one, JCheckBox two, JCheckBox three) {
        if (one.isSelected()) {
            two.setSelected(false);
            three.setSelected(false);
            fillInInfoPane();
        }
    }

    private void fillInInfoPane() {
        String text = "<html><font size=\"4\">";

        if (bukkitPluginCheckBox.isSelected()) {
            text += bukkitInfo;
            text += "<p/>";
        }

        if (spigotPluginCheckBox.isSelected()) {
            text += spigotInfo;
            text += "<p/>";
        }

        if (paperPluginCheckBox.isSelected()) {
            text += paperInfo;
            text += "<p/>";
        }

        if (spongePluginCheckBox.isSelected()) {
            text += spongeInfo;
            text += "<p/>";
        }

        if (forgeModCheckBox.isSelected()) {
            text += forgeInfo;
            text += "<p/>";
        }

        if (liteLoaderModCheckBox.isSelected()) {
            text += liteLoaderInfo;
            text += "<p/>";
        }

        if (bungeeCordPluginCheckBox.isSelected()) {
            text += bungeeCordInfo;
        }

        text += "</font></html>";

        infoPane.setText(text);
    }

    @Override
    public void updateDataModel() {
        creator.getSettings().clear();

        if (bukkitPluginCheckBox.isSelected()) {
            BukkitProjectConfiguration configuration = new BukkitProjectConfiguration();
            configuration.type = PlatformType.BUKKIT;
            creator.getSettings().put(PlatformType.BUKKIT, configuration);
        }

        if (spigotPluginCheckBox.isSelected()) {
            BukkitProjectConfiguration configuration = new BukkitProjectConfiguration();
            configuration.type = PlatformType.SPIGOT;
            creator.getSettings().put(PlatformType.BUKKIT, configuration);
        }

        if (paperPluginCheckBox.isSelected()) {
            BukkitProjectConfiguration configuration = new BukkitProjectConfiguration();
            configuration.type = PlatformType.PAPER;
            creator.getSettings().put(PlatformType.BUKKIT, configuration);
        }

        if (spongePluginCheckBox.isSelected()) {
            creator.getSettings().put(PlatformType.SPONGE, new SpongeProjectConfiguration());
        }

        if (forgeModCheckBox.isSelected()) {
            creator.getSettings().put(PlatformType.FORGE, new ForgeProjectConfiguration());
        }

        if (liteLoaderModCheckBox.isSelected()) {
            creator.getSettings().put(PlatformType.LITELOADER, new LiteLoaderProjectConfiguration());
        }

        if (bungeeCordPluginCheckBox.isSelected()) {
            creator.getSettings().put(PlatformType.BUNGEECORD, new BungeeCordProjectConfiguration());
        }

        creator.getSettings().values().iterator().next().isFirst = true;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return bukkitPluginCheckBox .isSelected() ||
            spigotPluginCheckBox    .isSelected() ||
            paperPluginCheckBox     .isSelected() ||
            spongePluginCheckBox    .isSelected() ||
            forgeModCheckBox        .isSelected() ||
            liteLoaderModCheckBox   .isSelected() ||
            bungeeCordPluginCheckBox.isSelected();
    }
}
