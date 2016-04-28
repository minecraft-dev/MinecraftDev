/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.creator;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MavenWizardStep extends ModuleWizardStep {

    private JTextField groupIdField;
    private JTextField artifactIdField;
    private JTextField versionField;
    private JPanel panel;

    private final MavenProjectCreator creator;

    public MavenWizardStep(@NotNull MavenProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {
    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();
        creator.setGroupId(groupIdField.getText());
        creator.setArtifactId(artifactIdField.getText());
        creator.setVersion(versionField.getText());
    }

    @Override
    public boolean validate() throws ConfigurationException {
        try {
            if (groupIdField.getText().trim().isEmpty()) {
                throw new EException(groupIdField);
            }

            if (artifactIdField.getText().trim().isEmpty()) {
                throw new EException(artifactIdField);
            }

            if (versionField.getText().trim().isEmpty()) {
                throw new EException(versionField);
            }
        } catch (EException e) {
            String message = "<html>Please fill in all fields</html>";
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                    .setFadeoutTime(4000)
                    .createBalloon()
                    .show(RelativePoint.getSouthWestOf(e.getJ()), Balloon.Position.below);
            return false;
        }
        return true;
    }

    class EException extends Exception {
        private JComponent j;

        public EException(JComponent j) {
            this.j = j;
        }

        public JComponent getJ() {
            return j;
        }
    }
}
