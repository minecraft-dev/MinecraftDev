package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.WordUtils;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SpongeProjectSettingsWizard extends ModuleWizardStep {

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

    private final SpongeProjectConfiguration settings = new SpongeProjectConfiguration();
    private final MinecraftProjectCreator creator;

    public SpongeProjectSettingsWizard(MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(this.creator.getGroupId() + '.' + this.creator.getArtifactId()
                + '.' + WordUtils.capitalizeFully(this.creator.getArtifactId()));

        if (UIUtil.isUnderDarcula()) {
            title.setIcon(PlatformAssets.SPONGE_ICON_2X);
        } else {
            title.setIcon(PlatformAssets.SPONGE_ICON_DARK_2X);
        }

        return panel;
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

        settings.generateDocumentedListeners = this.generateDocumentedListenersCheckBox.isSelected();

        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}
}
