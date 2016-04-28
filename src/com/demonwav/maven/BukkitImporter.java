/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.maven;

import com.demonwav.BukkitModuleType;

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
