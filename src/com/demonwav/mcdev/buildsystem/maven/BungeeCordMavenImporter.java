/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;

import org.jetbrains.annotations.NotNull;

public class BungeeCordMavenImporter extends MinecraftMavenImporter {
    public BungeeCordMavenImporter() {
        super("net.md-5", "bungeecord-api");
    }

    @NotNull
    @Override
    public BungeeCordModuleType getModuleType() {
        return BungeeCordModuleType.getInstance();
    }
}
