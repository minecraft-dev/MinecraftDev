/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AtMixinUtil {
    private AtMixinUtil() {}

    @Nullable
    public static PsiClass getClassFromString(@Nullable String text, @NotNull Project project) {
        if (text == null) {
            return null;
        }

        final JavaPsiFacade instance = JavaPsiFacade.getInstance(project);

        // We don't care about arrays
        text = text.replaceAll("\\[", "");

        if (text.startsWith("L")) {
            // class
            text = text.substring(1);

            text = text.replaceAll(";", "");
            text = text.replaceAll("/", ".");

            return instance.findClass(text, GlobalSearchScope.allScope(project));
        } else {
            // Primitive
            switch (text) {
                case "B":
                    return instance.findClass(Integer.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "C":
                    return instance.findClass(Character.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "D":
                    return instance.findClass(Double.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "F":
                    return instance.findClass(Float.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "I":
                    return instance.findClass(Integer.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "J":
                    return instance.findClass(Long.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "S":
                    return instance.findClass(Short.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                case "Z":
                    return instance.findClass(Boolean.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                default:
                    return null;
            }
        }
    }
}
