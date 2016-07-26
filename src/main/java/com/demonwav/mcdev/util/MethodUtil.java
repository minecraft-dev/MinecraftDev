package com.demonwav.mcdev.util;

import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.MethodSignature;
import org.jetbrains.annotations.NotNull;

public class MethodUtil {


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

}
