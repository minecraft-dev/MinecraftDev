/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;

import org.jetbrains.annotations.NotNull;

public class SpigotImporter extends BukkitImporter{
    public SpigotImporter() {
        super("org.spigotmc", "spigot-api");
    }

    public SpigotImporter(String groupId, String artifactId) {
        super(groupId, artifactId);
    }

    @NotNull
    @Override
    public SpigotModuleType getModuleType() {
        return SpigotModuleType.getInstance();
    }
}
