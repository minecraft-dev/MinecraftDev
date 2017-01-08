/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.util;

import com.demonwav.mcdev.platform.bukkit.BukkitModule;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class BukkitUtil {

    public static PsiFile getPluginYml(BukkitModule module) {
        return PsiManager.getInstance(module.getModule().getProject()).findFile(module.getPluginYml());
    }
}
