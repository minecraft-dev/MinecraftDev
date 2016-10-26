/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.canary.CanaryProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CanaryProjectSettingsWizard extends MinecraftModuleWizardStep {

    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JTextField authorsField;
    private JComboBox loadOrderBox;
    private JTextField dependField;
    private JLabel title;
    private JComboBox<String> canaryVersionBox;

    private CanaryProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    public CanaryProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        settings = (CanaryProjectConfiguration) creator.getSettings().get(PlatformType.CANARY);
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
            case CANARY:
                title.setIcon(PlatformAssets.CANARY_ICON_2X);
                title.setText("<html><font size=\"5\">Canary Settings</font></html>");
                break;
            case NEPTUNE:
                title.setIcon(PlatformAssets.NEPTUNE_ICON_2X);
                title.setText("<html><font size=\"5\">Neptune Settings</font></html>");
                break;
            default:
        }

        return panel;
    }

    @Override
    public boolean isStepVisible() {
        settings = (CanaryProjectConfiguration) creator.getSettings().get(PlatformType.CANARY);
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
        this.settings.setAuthors(this.authorsField.getText());
        this.settings.enableEarly = this.loadOrderBox.getSelectedIndex() == 0 ? false : true;
        this.settings.setDependencies(this.dependField.getText());
        this.settings.canaryVersion = (String) canaryVersionBox.getSelectedItem();
    }

    @Override
    public void updateDataModel() {}
}
