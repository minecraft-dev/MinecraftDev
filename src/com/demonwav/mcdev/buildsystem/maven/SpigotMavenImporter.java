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

public class SpigotMavenImporter extends BukkitMavenImporter {
    public SpigotMavenImporter() {
        super("org.spigotmc", "spigot-api");
    }

    public SpigotMavenImporter(String groupId, String artifactId) {
        super(groupId, artifactId);
    }



    @NotNull
    @Override
    public SpigotModuleType getModuleType() {
        return SpigotModuleType.getInstance();
    }
}
