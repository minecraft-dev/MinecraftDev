package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ForgeProjectSettingsWizard extends ModuleWizardStep {

    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JLabel title;
    private JTextField descriptionField;
    private JTextField authorsField;
    private JTextField websiteField;
    private JTextField dependField;
    private JTextField updateUrlField;

    private final ForgeProjectConfiguration settings = new ForgeProjectConfiguration();
    private final MinecraftProjectCreator creator;

    public ForgeProjectSettingsWizard(MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        return null;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        try {
            if (pluginNameField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("empty", pluginNameField);
            }

            if (pluginVersionField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("empty", pluginVersionField);
            }

            if (mainClassField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("empty", mainClassField);
            }
            if (!authorsField.getText().matches(ProjectSettingsWizardStep.pattern)) {
                throw new MinecraftSetupException("bad", authorsField);
            }

            if (!dependField.getText().matches(ProjectSettingsWizardStep.pattern)) {
                throw new MinecraftSetupException("bad", dependField);
            }
        } catch (MinecraftSetupException e) {
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
        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();

        settings.setAuthors(authorsField.getText());
        settings.setDependencies(dependField.getText());
        settings.description = descriptionField.getText();
        settings.website = websiteField.getText();
        settings.updateUrl = updateUrlField.getText();

        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}
}
