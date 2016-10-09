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

import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SpongeProjectSettingsWizard extends MinecraftModuleWizardStep {

    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JLabel title;
    private JTextField descriptionField;
    private JTextField authorsField;
    private JTextField websiteField;
    private JTextField dependField;
    private JCheckBox generateDocumentedListenersCheckBox;
    private JComboBox<String> spongeApiVersionBox;

    private SpongeProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    public SpongeProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        settings = (SpongeProjectConfiguration) creator.getSettings().get(PlatformType.SPONGE);
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
            mainClassField.setText(mainClassField.getText() + PlatformType.SPONGE.getNormalName());
        }

        return panel;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorsField, dependField, pattern);
    }

    @Override
    public boolean isStepVisible() {
        settings = (SpongeProjectConfiguration) creator.getSettings().get(PlatformType.SPONGE);
        return settings != null;
    }

    @Override
    public void onStepLeaving() {
        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();

        settings.setAuthors(authorsField.getText());
        settings.setDependencies(dependField.getText());
        settings.description = descriptionField.getText();
        settings.website = websiteField.getText();

        settings.generateDocumentedListeners = this.generateDocumentedListenersCheckBox.isSelected();
        settings.spongeApiVersion = (String) spongeApiVersionBox.getSelectedItem();
    }

    @Override
    public void updateDataModel() {}
}
