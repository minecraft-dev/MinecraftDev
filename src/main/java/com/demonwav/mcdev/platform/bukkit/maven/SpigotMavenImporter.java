/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.maven;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class SpigotMavenImporter extends BukkitMavenImporter {

    public SpigotMavenImporter() {
        super(SpigotModuleType.INSTANCE);
    }

    public SpigotMavenImporter(@NotNull final AbstractModuleType type) {
        super(type);
    }

    @NotNull
    @Override
    public ModuleType getModuleType() {
        return JavaModuleType.getModuleType();
    }
}
