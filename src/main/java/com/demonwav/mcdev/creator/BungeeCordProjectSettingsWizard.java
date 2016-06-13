package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
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

    public BungeeCordProjectSettingsWizard(@NotNull MinecraftProjectCreator creator, int index) {
        super();
        this.creator = creator;
        this.settings = (BungeeCordProjectConfiguration) creator.getSettings().get(index);
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());

        if (creator.index != 0) {
            pluginNameField.setEditable(false);
            pluginVersionField.setEditable(false);
        }

        mainClassField.setText(this.creator.getGroupId() + '.' + this.creator.getArtifactId()
                + '.' + WordUtils.capitalizeFully(this.creator.getArtifactId()));

        if (creator.getSettings().size() > 1) {
            mainClassField.setText(mainClassField.getText() + WordUtils.capitalizeFully(creator.getSettings().get(creator.index).type.name()));
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

    @Override
    public void setIndex(int index) {
        this.settings = (BungeeCordProjectConfiguration) creator.getSettings().get(index);
    }
}
