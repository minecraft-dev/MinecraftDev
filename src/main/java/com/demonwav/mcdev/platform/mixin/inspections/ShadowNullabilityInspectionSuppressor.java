/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.platform.mixin.util.MixinConstants;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShadowNullabilityInspectionSuppressor implements InspectionSuppressor {

    public static final String ID = "NullableProblems";

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!toolId.equals(ID)) {
            return false;
        }

        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return false;
        }

        final MinecraftModule instance = MinecraftModule.getInstance(module);
        if (instance == null) {
            return false;
        }

        if (!instance.isOfType(MixinModuleType.getInstance())) {
            return false;
        }

        if (!(element instanceof PsiIdentifier)) {
            return false;
        }

        if (!(element.getParent() instanceof PsiField)) {
            return false;
        }

        final PsiField field = (PsiField) element.getParent();

        final PsiModifierList modifierList = field.getModifierList();
        return modifierList != null && modifierList.findAnnotation(MixinConstants.Annotations.SHADOW) != null;
    }

    @NotNull
    @Override
    public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return new SuppressQuickFix[0];
    }
}
