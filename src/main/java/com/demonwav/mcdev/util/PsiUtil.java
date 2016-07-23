package com.demonwav.mcdev.util;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PsiUtil {

    @Nullable
    public static PsiClass getClassOfElement(@NotNull PsiElement element) {
        if (element instanceof PsiClass) {
            return (PsiClass) element;
        }

        while (element.getParent() != null) {

            if (element.getParent() instanceof PsiClass) {
                return (PsiClass) element.getParent();
            }

            element = element.getParent();
        }
        return null;
    }
}
