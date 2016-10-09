/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.bukkit.yaml;

import org.jetbrains.annotations.NotNull;

public final class PluginDescriptionFileConstants {

    @NotNull public static final String NAME = "name";
    @NotNull public static final String MAIN = "main";
    @NotNull public static final String VERSION = "version";
    @NotNull public static final String AUTHOR = "author";
    @NotNull public static final String DESCRIPTION = "description";
    @NotNull public static final String WEBSITE = "website";
    @NotNull public static final String PREFIX = "prefix";
    @NotNull public static final String LOAD = "load";
    @NotNull public static final String AUTHORS = "authors";
    @NotNull public static final String LOADBEFORE = "loadbefore";
    @NotNull public static final String DEPEND = "depend";
    @NotNull public static final String SOFTDEPEND = "softdepend";
    @NotNull public static final String COMMANDS = "commands";
    @NotNull public static final String PERMISSIONS = "permissions";
    @NotNull public static final String DATABASE = "database";

    private PluginDescriptionFileConstants() {
    }
}
