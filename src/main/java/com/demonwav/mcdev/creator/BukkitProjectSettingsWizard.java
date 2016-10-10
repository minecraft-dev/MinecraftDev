/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import static com.demonwav.mcdev.platform.PlatformType.BUKKIT;
import static com.demonwav.mcdev.platform.PlatformType.PAPER;
import static com.demonwav.mcdev.platform.PlatformType.SPIGOT;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration;
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder;

import com.intellij.openapi.options.ConfigurationException;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BukkitProjectSettingsWizard extends MinecraftModuleWizardStep {

    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JTextField descriptionField;
    private JTextField authorsField;
    private JTextField websiteField;
    private JTextField prefixField;
    private JComboBox loadOrderBox;
    private JTextField loadBeforeField;
    private JTextField dependField;
    private JTextField softDependField;
    private JLabel title;
    private JComboBox<String> minecraftVersionBox;

    private BukkitProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    public BukkitProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        settings = (BukkitProjectConfiguration) creator.getSettings().get(PlatformType.BUKKIT);
        if (settings == null) {
            return panel;
        }

        String name = WordUtils.capitalize(creator.getArtifactId());
        pluginNameField.setText(name);
        pluginVersionField.setText(creator.getVersion());

        if (settings != null && !settings.isFirst) {
            pluginNameField.setEditable(false);
            pluginVersionField.setEditable(false);
        }

        mainClassField.setText(creator.getGroupId().toLowerCase() + '.' + creator.getArtifactId().toLowerCase()
            + '.' + name);

        if (creator.getSettings().size() > 1) {
            mainClassField.setText(mainClassField.getText() + settings.type.getNormalName());
        }

        switch (settings.type) {
            case BUKKIT:
                title.setIcon(PlatformAssets.BUKKIT_ICON_2X);
                title.setText("<html><font size=\"5\">Bukkit Settings</font></html>");
                settings.type = BUKKIT;
                break;
            case SPIGOT:
                title.setIcon(PlatformAssets.SPIGOT_ICON_2X);
                title.setText("<html><font size=\"5\">Spigot Settings</font></html>");
                settings.type = SPIGOT;
                break;
            case PAPER:
                title.setIcon(PlatformAssets.PAPER_ICON_2X);
                title.setText("<html><font size=\"5\">Paper Settings</font></html>");
                settings.type = PAPER;
                break;
            default:
        }

        return panel;
    }

    @Override
    public boolean isStepVisible() {
        settings = (BukkitProjectConfiguration) creator.getSettings().get(PlatformType.BUKKIT);
        return settings != null;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, pattern);
    }

    @Override
    public void onStepLeaving() {
        this.settings.pluginName = pluginNameField.getText();
        this.settings.pluginVersion = pluginVersionField.getText();
        this.settings.mainClass = mainClassField.getText();
        this.settings.description = descriptionField.getText();
        this.settings.setAuthors(this.authorsField.getText());
        this.settings.website = websiteField.getText();
        this.settings.prefix = prefixField.getText();
        this.settings.loadOrder = this.loadOrderBox.getSelectedIndex() == 0 ? LoadOrder.POSTWORLD : LoadOrder.STARTUP;
        this.settings.setLoadBefore(this.loadBeforeField.getText());
        this.settings.setDependencies(this.dependField.getText());
        this.settings.setSoftDependencies(this.softDependField.getText());
        this.settings.minecraftVersion = (String) minecraftVersionBox.getSelectedItem();
    }

    @Override
    public void updateDataModel() {}
}
