/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.BukkitPlugin.project;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BukkitProjectSettingsWizardStep extends ModuleWizardStep {

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

    private S settings = new S();
    private MavenProjectCreator creator;

    public static class S {
        public enum L { STARTUP, POSTWORLD }

        public String pluginName;
        public String pluginVersion;
        public String mainClass;
        public String description;
        public String author;
        public String authorList;
        public String website;
        public String prefix;
        public boolean database;
        public L load;
        public String loadBefore;
        public String depend;
        public String softDepend;
    }

    public BukkitProjectSettingsWizardStep(MavenProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(creator.getArtifactId());
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(creator.getArtifactId());

        return panel;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        try {
            if (pluginNameField.getText().trim().isEmpty())
                throw new EException("empty", pluginNameField);

            if (pluginVersionField.getText().trim().isEmpty())
                throw new EException("empty", pluginVersionField);

            if (mainClassField.getText().trim().isEmpty())
                throw new EException("empty", mainClassField);

            if (!additionAuthorsField.getText().matches(pattern))
                throw new EException("bad", additionAuthorsField);

            if (!loadBeforeField.getText().matches(pattern))
                throw new EException("bad", loadBeforeField);

            if (!dependField.getText().matches(pattern))
                throw new EException("bad", dependField);

            if (!softDependField.getText().matches(pattern))
                throw new EException("bad", softDependField);
        } catch (EException e) {
            String message = "";
            switch (e.getMessage()) {
                case "empty":
                    message = "<html>Please fill in all required fields</html>";
                    break;
                case "bad":
                    message = "<html>Please enter author and plugin names as a comma separated list</html>";
                    break;
                default:
                    message = "<html>Unknown Error</html>";
                    break;
            }
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
        settings.authorList = additionAuthorsField.getText();
        settings.website = websiteField.getText();
        settings.prefix = prefixField.getText();
        settings.database = databaseBox.isSelected();
        settings.load = loadBox.getSelectedIndex() == 0 ? S.L.STARTUP : S.L.POSTWORLD;
        settings.loadBefore = loadBeforeField.getText();
        settings.depend = dependField.getText();
        settings.softDepend = softDependField.getText();
        creator.setSettings(settings);
    }

    @Override
    public void updateDataModel() {}

    class EException extends Exception {
        private JComponent j;
        public EException(String msg, JComponent j) {
            super(msg);
            this.j = j;
        }
        public JComponent getJ() {
            return j;
        }
    }
}
