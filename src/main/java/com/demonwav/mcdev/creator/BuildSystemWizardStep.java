package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem;
import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

// TODO gradle it up

public class BuildSystemWizardStep extends ModuleWizardStep {

    private JTextField groupIdField;
    private JTextField artifactIdField;
    private JTextField versionField;
    private JPanel panel;
    private JComboBox<String> buildSystemBox;

    private final MinecraftProjectCreator creator;

    public BuildSystemWizardStep(@NotNull MinecraftProjectCreator creator) {
        super();
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        switch (creator.getType()) {
            case BUKKIT:
            case SPIGOT:
            case PAPER:
            case BUNGEECORD:
                buildSystemBox.setSelectedIndex(0);
                buildSystemBox.setVisible(true);
                break;
            case SPONGE:
                buildSystemBox.setSelectedIndex(1);
                buildSystemBox.setVisible(true);
                break;
            case FORGE:buildSystemBox.setSelectedIndex(1);
                buildSystemBox.setVisible(false);
                break;
        }
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
        BuildSystem buildSystem;
        if (buildSystemBox.getSelectedIndex() == 0) {
            buildSystem = new MavenBuildSystem();
        } else {
            buildSystem = new GradleBuildSystem();
        }
        // Java 8 always
        buildSystem.setBuildVersion("1.8");
        creator.setBuildSystem(buildSystem);
    }

    @Override
    public boolean validate() throws ConfigurationException {
        try {
            if (groupIdField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("fillAll", groupIdField);
            }

            if (artifactIdField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("fillAll", artifactIdField);
            }

            if (versionField.getText().trim().isEmpty()) {
                throw new MinecraftSetupException("fillAll", versionField);
            }

            if (creator.getType() == PlatformType.FORGE && buildSystemBox.getSelectedIndex() == 0) {
                throw new MinecraftSetupException("Forge does not support Maven", buildSystemBox);
            }
        } catch (MinecraftSetupException e) {
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(e.getError(), MessageType.ERROR, null)
                    .setFadeoutTime(2000)
                    .createBalloon()
                    .show(RelativePoint.getSouthWestOf(e.getJ()), Balloon.Position.below);
            return false;
        }
        return true;
    }
}
