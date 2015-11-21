/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.creator;

import com.demonwav.bukkitplugin.exceptions.BukkitSetupException;
import com.demonwav.bukkitplugin.util.ProjectSettings;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BungeeCordProjectSettingsWizard extends ModuleWizardStep {

    private static final String pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?";

    private JPanel panel;
    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JTextField descriptionField;
    private JTextField authorField;
    private JTextField dependField;
    private JTextField softDependField;

    private final ProjectSettings settings = new ProjectSettings();
    private final MavenProjectCreator creator;

    public BungeeCordProjectSettingsWizard(@NotNull MavenProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));

        return panel;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        try {
            if (pluginNameField.getText().trim().isEmpty()) {
                throw new BukkitSetupException("empty", pluginNameField);
            }

            if (pluginVersionField.getText().trim().isEmpty()) {
                throw new BukkitSetupException("empty", pluginVersionField);
            }

            if (mainClassField.getText().trim().isEmpty()) {
                throw new BukkitSetupException("empty", mainClassField);
            }

            if (!dependField.getText().matches(pattern)) {
                throw new BukkitSetupException("bad", dependField);
            }

            if (!softDependField.getText().matches(pattern)) {
                throw new BukkitSetupException("bad", softDependField);
            }
        } catch (BukkitSetupException e) {
            String message = e.getError();

            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                    .setFadeoutTime(4000)
                    .createBalloon()
                    .show(RelativePoint.getSouthWestOf(e.getJ()), Balloon.Position.below);
            return false;
        }
        return true;
    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();
        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();
        settings.description = descriptionField.getText();
        settings.author = authorField.getText();
        settings.depend = new ArrayList<>(Arrays.asList(dependField.getText().trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*")));
        settings.softDepend = new ArrayList<>(Arrays.asList(softDependField.getText().trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*")));
        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}
}
