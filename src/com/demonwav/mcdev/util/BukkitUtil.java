package com.demonwav.mcdev.util;

import com.demonwav.mcdev.BukkitProject;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class BukkitUtil {

    public static PsiFile getPluginYml(BukkitProject project) {
        return PsiManager.getInstance(project.getProject()).findFile(project.getPluginYml());
    }
}
