/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.creator;

import com.demonwav.bukkitplugin.BukkitProject.Type;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class ProjectSettingsWizardStep extends ModuleWizardStep {

    private MavenProjectCreator creator;
    private ModuleWizardStep wizard;
    private Type type;

    public ProjectSettingsWizardStep(@NotNull MavenProjectCreator creator) {
        this.creator = creator;
        type = creator.getType();
    }

    @Override
    public JComponent getComponent() {
        if (wizard == null || creator.getType() != type) {
            // detect type changes
            type = creator.getType();
            if (creator.getType() == Type.BUNGEECORD) {
                wizard = new BungeeCordProjectSettingsWizard(creator);
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
    public void updateDataModel() {}
}
