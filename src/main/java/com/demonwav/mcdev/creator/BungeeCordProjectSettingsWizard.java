/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BungeeCordProjectSettingsWizard extends MinecraftModuleWizardStep {

    private static final String pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?";

    private JPanel panel;
    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JTextField descriptionField;
    private JTextField authorField;
    private JTextField dependField;
    private JTextField softDependField;
    private JComboBox<String> minecraftVersionBox;

    private BungeeCordProjectConfiguration settings;
    private final MinecraftProjectCreator creator;

    public BungeeCordProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        settings = (BungeeCordProjectConfiguration) creator.getSettings().get(PlatformType.BUNGEECORD);
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

        mainClassField.setText(creator.getGroupId().toLowerCase() + '.' + creator.getArtifactId().toLowerCase() + '.' + name);

        if (creator.getSettings().size() > 1) {
            mainClassField.setText(mainClassField.getText() + PlatformType.BUNGEECORD.getNormalName());
        }

        return panel;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return validate(pluginNameField, pluginVersionField, mainClassField, authorField, dependField, pattern);
    }

    @Override
    public boolean isStepVisible() {
        settings = (BungeeCordProjectConfiguration) creator.getSettings().get(PlatformType.BUNGEECORD);
        return settings != null;
    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();
        this.settings.pluginName = pluginNameField.getText();
        this.settings.pluginVersion = pluginVersionField.getText();
        this.settings.mainClass = mainClassField.getText();
        this.settings.description = descriptionField.getText();
        this.settings.setAuthors(this.authorField.getText());
        this.settings.setDependencies(this.dependField.getText());
        this.settings.setSoftDependencies(this.softDependField.getText());
        this.settings.minecraftVersion = (String) minecraftVersionBox.getSelectedItem();
    }

    @Override
    public void updateDataModel() {}
}
