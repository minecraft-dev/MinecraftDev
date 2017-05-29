/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util;

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class KotlinIsBroken {

    private KotlinIsBroken() {}

    // See https://youtrack.jetbrains.com/issue/KT-16424
    public static void removeAnnotationAttribute(@NotNull PsiAnnotation annotation, @Nullable String name) {
        annotation.setDeclaredAttributeValue(name, null);
    }
}
