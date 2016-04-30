/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev;

import com.demonwav.mcdev.icons.BukkitProjectsIcons;
import com.demonwav.mcdev.creator.BukkitModuleBuilder;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class BukkitModuleType extends JavaModuleType {

    private static final String ID = "BUKKIT_MODULE_TYPE";

    public BukkitModuleType() {
        super(ID);
    }

    public static BukkitModuleType getInstance() {
        return (BukkitModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @NotNull
    @Override
    public BukkitModuleBuilder createModuleBuilder() {
        return new BukkitModuleBuilder();
    }

    @NotNull
    @Override
    public String getName() {
        return "Bukkit Project";
    }

//    @NotNull
//    @Override
//    public String getDescription() {
//        return "Create standard Maven Bukkit, Spigot, Paper, BungeeCord, Sponge, or Forge projects";
//    }

    @Override
    public Icon getBigIcon() {
        return BukkitProjectsIcons.BukkitProjectBig;
    }

    @Override
    public Icon getIcon() {
        return BukkitProjectsIcons.BukkitProject;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return BukkitProjectsIcons.BukkitProject;
    }
}
