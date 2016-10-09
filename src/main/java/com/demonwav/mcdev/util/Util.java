/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class Util {

    public static void runWriteTask(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() ->
            ApplicationManager.getApplication().runWriteAction(runnable), ModalityState.NON_MODAL);
    }

    public static void runWriteTaskLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(() ->
            ApplicationManager.getApplication().runWriteAction(runnable), ModalityState.NON_MODAL);
    }

    public static void invokeLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    public static String defaultNameForSubClassEvents(@NotNull PsiClass psiClass) {
        boolean isInnerClass = !(psiClass.getParent() instanceof PsiFile);

        StringBuilder name = new StringBuilder();
        if (isInnerClass) {
            PsiClass containingClass = PsiUtil.getContainingNotInnerClass(psiClass);
            if (containingClass != null) {
                if (containingClass.getName() != null) {
                    name.append(containingClass.getName().replaceAll("Event", ""));
                }
            }
        }

        String className = psiClass.getName();
        assert className != null;
        if (className.startsWith(name.toString())) {
            className = className.substring(name.length());
        }
        name.append(className.replaceAll("Event", ""));

        name.insert(0, "on");
        return name.toString();
    }
}
