package com.demonwav.mcdev.util;

import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.MethodSignature;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class McMethodUtil {


    public static boolean areSignaturesEqualLightweight(@NotNull MethodSignature sig1,
                                                        @NotNull MethodSignature sig2,
                                                        @NotNull String sig2NameReplacement) {
        final boolean isConstructor1 = sig1.isConstructor();
        final boolean isConstructor2 = sig2.isConstructor();
        if (isConstructor1 != isConstructor2) {
            return false;
        }

        if (!isConstructor1 || !(sig1 instanceof HierarchicalMethodSignature || sig2 instanceof HierarchicalMethodSignature)) {
            final String name1 = sig1.getName();
            final String name2 = sig2NameReplacement;
            if (!name1.equals(name2)) {
                return false;
            }
        }

        final PsiType[] parameterTypes1 = sig1.getParameterTypes();
        final PsiType[] parameterTypes2 = sig2.getParameterTypes();
        if (parameterTypes1.length != parameterTypes2.length) return false;

        // optimization: check for really different types in method parameters
        for (int i = 0; i < parameterTypes1.length; i++) {
            final PsiType type1 = parameterTypes1[i];
            final PsiType type2 = parameterTypes2[i];
            if (type1 instanceof PsiPrimitiveType != type2 instanceof PsiPrimitiveType) {
                return false;
            }
            if (type1 instanceof PsiPrimitiveType && !type1.equals(type2)) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiMethod getContainingMethod(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }

        // Class, File, and Directory, if we've hit that, we're not in a method
        // Member, if we've hit that immediately after failing the "are we a method" check, then we
        // aren't in a method
        if (element instanceof PsiClass || element instanceof PsiMember || element instanceof PsiFile || element instanceof PsiDirectory) {
            return null;
        }

        final PsiElement parent = element.getParent();
        if (parent == null) {
            return null;
        }

        return getContainingMethod(parent);
    }
}
