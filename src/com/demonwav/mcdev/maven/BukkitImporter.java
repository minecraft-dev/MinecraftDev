/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.maven;

import com.demonwav.mcdev.BukkitModuleType;

import org.jetbrains.annotations.NotNull;

public class BukkitImporter extends Importer {
    public BukkitImporter() {
        super("org.bukkit", "bukkit");
    }

    @NotNull
    @Override
    public BukkitModuleType getModuleType() {
        return BukkitModuleType.getInstance();
    }
}
