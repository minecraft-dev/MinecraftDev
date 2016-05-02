package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exceptions.MinecraftSetupException;
import com.demonwav.mcdev.util.BukkitSettings;

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

import javax.swing.JCheckBox;
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
    private JTextField authorField;
    private JTextField additionAuthorsField;
    private JTextField websiteField;
    private JTextField prefixField;
    private JCheckBox databaseBox;
    private JComboBox loadBox;
    private JTextField loadBeforeField;
    private JTextField dependField;
    private JTextField softDependField;
    private JLabel title;

    private BukkitSettings settings = new BukkitSettings();
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
        }

        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));

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
        settings.author = authorField.getText();
        settings.authorList = new ArrayList<>(Arrays.asList(additionAuthorsField.getText().trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*")));
        settings.website = websiteField.getText();
        settings.prefix = prefixField.getText();
        settings.database = databaseBox.isSelected();
        settings.load = loadBox.getSelectedIndex() == 0 ? BukkitSettings.Load.POSTWORLD : BukkitSettings.Load.STARTUP;
        settings.loadBefore = new ArrayList<>(Arrays.asList(loadBeforeField.getText().trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*")));
        settings.depend = new ArrayList<>(Arrays.asList(dependField.getText().trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*")));
        settings.softDepend = new ArrayList<>(Arrays.asList(softDependField.getText().trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*")));
        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}
}
