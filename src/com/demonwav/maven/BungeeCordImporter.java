/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.maven;

import com.demonwav.BungeeCordModuleType;

import org.jetbrains.annotations.NotNull;

public class BungeeCordImporter extends Importer {
    public BungeeCordImporter() {
        super("net.md-5", "bungeecord-api");
    }

    @NotNull
    @Override
    public BungeeCordModuleType getModuleType() {
        return BungeeCordModuleType.getInstance();
    }
}
