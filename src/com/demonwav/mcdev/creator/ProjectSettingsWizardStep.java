package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.platform.Type;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * This class serves as an in-between for BukkitSettingsWizardStep, BungeeCordSettingsWizardStep, SpongeSettingsWizardStep,
 * and any other classes which might make sense to go here. This single ProjectSettingsWizardStep class is used in
 * place of any of those, and it chooses which WizardStep to show based on the current project type selected.
 */
public class ProjectSettingsWizardStep extends ModuleWizardStep {

    private MinecraftProjectCreator creator;
    private ModuleWizardStep wizard;
    private Type type;

    public ProjectSettingsWizardStep(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
        type = creator.getType();
    }

    @Override
    public JComponent getComponent() {
        // We don't want to recreate the wizard (and nuke the settings) if the type hasn't changed
        if (wizard == null || creator.getType() != type) {
            // detect type changes
            type = creator.getType();
            if (creator.getType() == Type.BUNGEECORD) {
                wizard = new BungeeCordProjectSettingsWizard(creator);
            } else if (creator.getType() == Type.SPONGE) {
                wizard = new SpongeProjectSettingsWizard(creator);
            } else {
                wizard = new BukkitProjectSettingsWizard(creator);
            }
        }
        return wizard.getComponent();
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return wizard.validate();
    }

    @Override
    public void onStepLeaving() {
        wizard.onStepLeaving();
    }

    @Override
    public void updateDataModel() {
    }
}
