package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;

public class ProjectChooserWizardStep extends ModuleWizardStep {

    private final MinecraftProjectCreator creator;
    private final ProjectSettingsWizardStep[] steps;

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

    private static final String bukkitInfo = "Create a standard " +
            "<a href=\"http://bukkit.org/\">Bukkit</a> plugin, for use " +
            "on CraftBukkit, Spigot, and Paper servers.";
    private static final String spigotInfo = "Create a standard " +
            "<a href=\"https://www.spigotmc.org/\">Spigot</a> plugin, for use " +
            "on Spigot and Paper servers.";
    private static final String paperInfo = "Create a standard " +
            "<a href=\"https://paper.emc.gs\">Paper</a> plugin, for use " +
            "on Paper servers.";
    private static final String bungeeCordInfo = "Create a standard " +
            "<a href=\"https://www.spigotmc.org/wiki/bungeecord/\"> BungeeCord</a> plugin, for use " +
            "on BungeeCord servers.";
    private static final String spongeInfo = "Create a standard " +
            "<a href=\"https://www.spongepowered.org/\"> Sponge</a> plugin, for use " +
            "on Sponge servers.";
    private static final String forgeInfo = "Create a standard " +
            "<a href=\"http://files.minecraftforge.net/\"> Forge</a> mod, for use " +
            "on Forge servers.";

    public ProjectChooserWizardStep(@NotNull MinecraftProjectCreator creator, @NotNull ProjectSettingsWizardStep[] steps) {
        super();
        this.creator = creator;
        this.steps = steps;
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

        bungeeCordPluginCheckBox.addActionListener(e ->
                fillInInfoPane()
        );

        // show the right sponge icon
        if (UIUtil.isUnderDarcula()) {
            spongeIcon.setIcon(PlatformAssets.SPONGE_ICON_2X);
        } else {
            spongeIcon.setIcon(PlatformAssets.SPONGE_ICON_DARK_2X);
        }

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
            creator.getSettings().add(configuration);
        }

        if (spigotPluginCheckBox.isSelected()) {
            BukkitProjectConfiguration configuration = new BukkitProjectConfiguration();
            configuration.type = PlatformType.SPIGOT;
            creator.getSettings().add(configuration);
        }

        if (paperPluginCheckBox.isSelected()) {
            BukkitProjectConfiguration configuration = new BukkitProjectConfiguration();
            configuration.type = PlatformType.PAPER;
            creator.getSettings().add(configuration);
        }

        if (spongePluginCheckBox.isSelected()) {
            creator.getSettings().add(new SpongeProjectConfiguration());
        }

        if (forgeModCheckBox.isSelected()) {
            creator.getSettings().add(new ForgeProjectConfiguration());
        }

        if (bungeeCordPluginCheckBox.isSelected()) {
            creator.getSettings().add(new BungeeCordProjectConfiguration());
        }

        creator.index = 0;
        for (ProjectSettingsWizardStep step : steps) {
            step.resetIndex();
        }
    }
}
