package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.util.Iterator;

public class SpongeForgeChooser extends ModuleWizardStep {
    private JPanel panel;
    private JRadioButton singleRadioButton;
    private JRadioButton multiRadioButton;
    private JLabel title;

    private final MinecraftProjectCreator creator;

    public SpongeForgeChooser(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        title.setIcon(PlatformAssets.SPONGE_FORGE_ICON_2X);

        return panel;
    }

    @Override
    public void updateDataModel() {}

    @Override
    public boolean isStepVisible() {
        // Only show this if both Sponge and Forge are selected
        return creator.getSettings().stream().filter(configuration ->
                configuration instanceof ForgeProjectConfiguration || configuration instanceof SpongeProjectConfiguration
        ).count() >= 2 || creator.getSettings().stream().anyMatch(configuration -> configuration instanceof SpongeForgeProjectConfiguration);
    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();

        if (singleRadioButton.isSelected()) {
            // First remove the singular forge and sponge configurations
            Iterator<ProjectConfiguration> configurationIterator = creator.getSettings().iterator();
            while (configurationIterator.hasNext()) {
                ProjectConfiguration configuration = configurationIterator.next();
                if (configuration instanceof ForgeProjectConfiguration || configuration instanceof SpongeProjectConfiguration) {
                    configurationIterator.remove();
                }
            }

            // Now add the combined SpongeForgeProjectConfiguration only if it's not already there
            if (!creator.getSettings().stream().anyMatch(configuration -> configuration instanceof SpongeForgeProjectConfiguration)) {
                creator.getSettings().add(new SpongeForgeProjectConfiguration());
            }
        } else {
            // First remove the multi sponge forge configuration
            Iterator<ProjectConfiguration> configurationIterator = creator.getSettings().iterator();
            while (configurationIterator.hasNext()) {
                ProjectConfiguration configuration = configurationIterator.next();
                if (configuration instanceof SpongeForgeProjectConfiguration) {
                    configurationIterator.remove();
                }
            }

            // Now add Forge and Sponge configurations respectively, but only if they aren't already there
            if (!creator.getSettings().stream().anyMatch(configuration -> configuration instanceof ForgeProjectConfiguration)) {
                creator.getSettings().add(new ForgeProjectConfiguration());
            }
            if (!creator.getSettings().stream().anyMatch(configuration -> configuration instanceof SpongeProjectConfiguration)) {
                creator.getSettings().add(new SpongeProjectConfiguration());
            }
        }
    }
}
