/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin;

import com.demonwav.bukkitplugin.icons.BukkitProjectsIcons;
import com.demonwav.bukkitplugin.creator.BukkitModuleBuilder;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class BukkitModuleType extends JavaModuleType {

    private static final String ID = "BUKKIT_MODULE_TYPE";
    private BukkitProject project = new BukkitProject();

    public BukkitModuleType() {
        super(ID);
    }

    public static BukkitModuleType getInstance() {
        return (BukkitModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @NotNull
    @Override
    public BukkitModuleBuilder createModuleBuilder() {
        return new BukkitModuleBuilder(project);
    }

    @NotNull
    @Override
    public String getName() {
        return "Bukkit Project";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Create standard Maven Bukkit, Spigot, or BungeeCord projects";
    }

    @Override
    public Icon getBigIcon() {
        if (project.getProjectType() == BukkitProject.Type.BUKKIT)
            return BukkitProjectsIcons.BukkitProjectBig;
        else
            return BukkitProjectsIcons.SpigotProjectBig;
    }

    @Override
    public Icon getIcon() {
        if (project.getProjectType() == BukkitProject.Type.BUKKIT)
            return BukkitProjectsIcons.BukkitProject;
        else
            return BukkitProjectsIcons.SpigotProject;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        if (project.getProjectType() == BukkitProject.Type.BUKKIT)
            return BukkitProjectsIcons.BukkitProject;
        else
            return BukkitProjectsIcons.SpigotProject;
    }


}
