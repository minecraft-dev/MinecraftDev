package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BukkitProjectSettingsWizard extends ModuleWizardStep {

    private static final String pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?";

    private JPanel panel;
    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JTextField descriptionField;
    private JTextField authorsField;
    @Deprecated private JTextField additionAuthorsField;
    private JTextField websiteField;
    private JTextField prefixField;
    private JComboBox loadBox;
    private JTextField loadBeforeField;
    private JTextField dependField;
    private JTextField softDependField;
    private JLabel title;

    private BukkitProjectConfiguration settings = new BukkitProjectConfiguration();
    private MinecraftProjectCreator creator;

    public BukkitProjectSettingsWizard(@NotNull MinecraftProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        switch (creator.getType()) {
            case BUKKIT:
                title.setText("<html><font size=\"5\">Bukkit Settings</font></html>");
                break;
            case SPIGOT:
                title.setText("<html><font size=\"5\">Spigot Settings</font></html>");
                break;
            case PAPER:
                title.setText("<html><font size=\"5\">Paper Settings</font></html>");
                break;
        }

        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(this.creator.getGroupId() + '.' + this.creator.getArtifactId()
                + '.' + WordUtils.capitalizeFully(this.creator.getArtifactId()));

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

            if (!additionAuthorsField.getText().matches(pattern)) {
                throw new MinecraftSetupException("bad", additionAuthorsField);
            }

            if (!loadBeforeField.getText().matches(pattern)) {
                throw new MinecraftSetupException("bad", loadBeforeField);
            }

            if (!dependField.getText().matches(pattern)) {
                throw new MinecraftSetupException("bad", dependField);
            }

            if (!softDependField.getText().matches(pattern)) {
                throw new MinecraftSetupException("bad", softDependField);
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
        super.onStepLeaving();
        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();
        settings.description = descriptionField.getText();
        this.settings.setAuthors(this.authorsField.getText());
        settings.website = websiteField.getText();
        settings.prefix = prefixField.getText();
        settings.load = loadBox.getSelectedIndex() == 0 ? BukkitProjectConfiguration.Load.POSTWORLD : BukkitProjectConfiguration.Load.STARTUP;
        this.settings.setLoadBefore(this.loadBeforeField.getText());
        this.settings.setDependencies(this.dependField.getText());
        this.settings.setSoftDependencies(this.softDependField.getText());
        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}
}
