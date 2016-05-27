package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * This class serves as an in-between for BukkitSettingsWizardStep, BungeeCordSettingsWizardStep, SpongeSettingsWizardStep,
 * and any other classes which might make sense to go here. This single ProjectSettingsWizardStep class is used in
 * place of any of those, and it chooses which WizardStep to show based on the current project types selected.
 */
public class ProjectSettingsWizardStep extends ModuleWizardStep {

    public static final String pattern = "(\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*,?|\\[?\\s*(\\w+)\\s*(,\\s*\\w+\\s*)*])?";

    private MinecraftProjectCreator creator;
    private ModuleWizardStep wizard;
    private PlatformType type;
    private int index = -1;

    public ProjectSettingsWizardStep(@NotNull MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        // If size == 0 then we aren't actually initialized yet
        if (creator.getSettings().size() == 0) {
            return null;
        }

        if (index == -1) {
            // This is first load, so we know two things
            //   1. The user got to this by clicking next
            //   2. The index will be he current index instance on the creator
            // So we will set it likewise
            index = creator.index;
        } else {
            // This is not first load, so we know our index. At this point we
            // don't know how we got here, so we tell the creator which index we
            // are instead of getting the index from it.
            creator.index = index;
        }

        if (creator.index == creator.getSettings().size()) {
            return null;
        }

        // Grab all type changes
        PlatformType newType = creator.getSettings().get(creator.index).type;
        // We don't want to recreate the wizard (and nuke the settings) if the type hasn't changed
        if (wizard == null || newType != this.type) {
            // remember what type we are now, so we know if it changes later
            this.type = newType;
            if (newType == PlatformType.BUNGEECORD) {
                wizard = new BungeeCordProjectSettingsWizard(creator);
            } else if (newType == PlatformType.SPONGE) {
                wizard = new SpongeProjectSettingsWizard(creator);
            } else if (newType == PlatformType.FORGE) {
                wizard = new ForgeProjectSettingsWizard(creator);
            } else {
                wizard = new BukkitProjectSettingsWizard(creator);
            }
        }
        return wizard.getComponent();
    }

    @Override
    public void updateStep() {}

    @Override
    public boolean isStepVisible() {
        return creator.index < creator.getSettings().size();
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (wizard == null || wizard.validate()) {
            creator.index++;
            return true;
        }
        return false;
    }

    @Override
    public void onStepLeaving() {
        if (wizard != null) {
            wizard.onStepLeaving();
        }
    }

    @Override
    public void updateDataModel() {
    }
}
