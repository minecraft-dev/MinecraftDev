/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.SpigotModuleType;

import org.jetbrains.annotations.NotNull;

public class SpigotImporter extends BukkitImporter{
    public SpigotImporter() {
        super("org.spigotmc", "spigot-api");
    }

    @NotNull
    @Override
    public SpigotModuleType getModuleType() {
        return SpigotModuleType.getInstance();
    }
}
