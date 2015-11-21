package com.demonwav.bukkitplugin.util;

import com.demonwav.bukkitplugin.BukkitProject;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class BukkitUtil {

    public static boolean isUltimate() {
        String prop = System.getProperty("idea.platform.prefix");
        return prop == null || prop.trim().isEmpty();
    }

    public static PsiFile getPluginYml(BukkitProject project) {
        return PsiManager.getInstance(project.getProject()).findFile(project.getPluginYml());
    }
}
