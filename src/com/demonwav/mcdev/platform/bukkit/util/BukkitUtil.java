package com.demonwav.mcdev.platform.bukkit.util;

import com.demonwav.mcdev.platform.bukkit.BukkitProject;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class BukkitUtil {

    public static PsiFile getPluginYml(BukkitProject project) {
        return PsiManager.getInstance(project.getProject()).findFile(project.getPluginYml());
    }
}
