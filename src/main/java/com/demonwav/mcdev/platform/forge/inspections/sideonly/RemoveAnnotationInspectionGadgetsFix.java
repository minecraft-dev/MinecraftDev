/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.platform.forge.util.ForgeConstants;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RemoveAnnotationInspectionGadgetsFix extends InspectionGadgetsFix {

    @Nullable
    public abstract PsiModifierListOwner getListOwner();

    @Override
    protected void doFix(Project project, ProblemDescriptor descriptor) {
        final PsiModifierListOwner owner = getListOwner();
        if (owner == null) {
            return;
        }

        final PsiModifierList list = owner.getModifierList();
        if (list == null) {
            return;
        }

        final PsiAnnotation annotation = list.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION);
        if (annotation == null) {
            return;
        }

        annotation.delete();
    }

    @Nls
    @NotNull
    @Override
    public abstract String getName();

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return getName();
    }
}
