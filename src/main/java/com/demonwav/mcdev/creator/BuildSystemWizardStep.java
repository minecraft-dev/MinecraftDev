/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.creator;

import static com.demonwav.mcdev.platform.PlatformType.FORGE;
import static com.demonwav.mcdev.platform.PlatformType.LITELOADER;
import static com.demonwav.mcdev.platform.PlatformType.SPONGE;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem;
import com.demonwav.mcdev.exception.MinecraftSetupException;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;

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
        return panel;
    }

    @Override
    public void updateStep() {
        if (creator.getSettings().size() > 1) {
            buildSystemBox.setSelectedIndex(1);
            buildSystemBox.setVisible(false);
            return;
        }
        if (creator.getSettings().values().stream().anyMatch(s -> s.type == FORGE) ||
                creator.getSettings().values().stream().anyMatch(s -> s.type == LITELOADER) ||
                creator.getSettings().values().stream().anyMatch(s -> s instanceof SpongeForgeProjectConfiguration)) {
            buildSystemBox.setSelectedIndex(1);
            buildSystemBox.setVisible(false);
        } else if (creator.getSettings().values().stream().anyMatch(s -> s.type == SPONGE)) {
            buildSystemBox.setSelectedIndex(1);
            buildSystemBox.setVisible(true);
        } else {
            buildSystemBox.setSelectedIndex(0);
            buildSystemBox.setVisible(true);
        }
    }

    @Override
    public void updateDataModel() {}

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

            if (creator.getSettings().values().stream().anyMatch(s -> s.type == FORGE) && buildSystemBox.getSelectedIndex() == 0) {
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
