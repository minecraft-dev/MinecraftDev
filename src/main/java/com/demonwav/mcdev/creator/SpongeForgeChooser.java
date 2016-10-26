/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class SpongeForgeChooser extends ModuleWizardStep {

    private JPanel panel;
    private JRadioButton singleRadioButton;
    @SuppressWarnings("unused")
    private JRadioButton multiRadioButton;
    private JLabel title;

    private final MinecraftProjectCreator creator;

    public SpongeForgeChooser(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        if (UIUtil.isUnderDarcula()) {
            title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_2X_DARK);
        } else {
            title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_2X);
        }

        return panel;
    }

    @Override
    public void updateDataModel() {}

    @Override
    public boolean isStepVisible() {
        // Only show this if both Sponge and Forge are selected
        return creator.getSettings().values().stream().filter(configuration ->
            configuration instanceof ForgeProjectConfiguration || configuration instanceof SpongeProjectConfiguration
        ).count() >= 2 || creator.getSettings().values().stream().anyMatch(conf -> conf instanceof SpongeForgeProjectConfiguration);
    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();

        if (singleRadioButton.isSelected()) {
            // First remove the singular forge and sponge configurations
            Iterator<ProjectConfiguration> configurationIterator = creator.getSettings().values().iterator();
            while (configurationIterator.hasNext()) {
                ProjectConfiguration configuration = configurationIterator.next();
                if (configuration instanceof ForgeProjectConfiguration || configuration instanceof SpongeProjectConfiguration) {
                    configurationIterator.remove();
                }
            }

            // Now add the combined SpongeForgeProjectConfiguration only if it's not already there
            if (!creator.getSettings().values().stream().anyMatch(configuration -> configuration instanceof SpongeForgeProjectConfiguration)) {
                creator.getSettings().put(PlatformType.FORGE, new SpongeForgeProjectConfiguration());
            }
        } else {
            // First remove the multi sponge forge configuration
            Iterator<ProjectConfiguration> configurationIterator = creator.getSettings().values().iterator();
            while (configurationIterator.hasNext()) {
                ProjectConfiguration configuration = configurationIterator.next();
                if (configuration instanceof SpongeForgeProjectConfiguration) {
                    configurationIterator.remove();
                }
            }

            // Now add Forge and Sponge configurations respectively, but only if they aren't already there
            if (!creator.getSettings().values().stream().anyMatch(configuration -> configuration instanceof ForgeProjectConfiguration)) {
                creator.getSettings().put(PlatformType.FORGE, new ForgeProjectConfiguration());
            }
            if (!creator.getSettings().values().stream().anyMatch(configuration -> configuration instanceof SpongeProjectConfiguration)) {
                creator.getSettings().put(PlatformType.SPONGE, new SpongeProjectConfiguration());
            }
        }
    }
}
