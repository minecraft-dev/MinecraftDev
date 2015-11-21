/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.maven;

import com.demonwav.bukkitplugin.SpigotModuleType;

import org.jetbrains.annotations.NotNull;

public class SpigotImporter extends Importer {
    public SpigotImporter() {
        super("org.spigotmc", "spigot-api");
    }

    @NotNull
    @Override
    public SpigotModuleType getModuleType() {
        return SpigotModuleType.getInstance();
    }
}
