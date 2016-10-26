/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierListOwner;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NestedClassSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of @SideOnly in nested class declaration";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return "A nested class cannot declare a side that is different from the parent class." +
            "\nEither remove the nested class's @SideOnly annotation, or change it to match it's parent's side.";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Classes which are annotated with @SideOnly cannot contain any nested classes which are annotated with a different " +
            "@SideOnly annotation. Since a class that is annotated with @SideOnly brings everything with it, @SideOnly annotated nested " +
            "classes are usually useless.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        final PsiClass psiClass = (PsiClass) infos[0];

        if (psiClass.isWritable()) {
            return new RemoveAnnotationInspectionGadgetsFix() {
                @Nullable
                @Override
                public PsiModifierListOwner getListOwner() {
                    return psiClass;
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Remove @SideOnly annotation from nested class";
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitClass(PsiClass aClass) {
                if (aClass.getParent() == null) {
                    return;
                }

                final PsiIdentifier identifier = aClass.getNameIdentifier();
                if (identifier == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(aClass)) {
                    return;
                }

                final List<Pair<Side, PsiClass>> classHierarchyList = SideOnlyUtil.checkClassHierarchy(aClass);

                // The class lists are ordered from lowest to highest in the hierarchy - that is the first element in the list
                // is the most nested class, and the last element in the list is the top level class
                //
                // In this case, the higher-level classes take precedence, so if a class is annotated as @SideOnly.CLIENT and a nested class is
                // annotated as @SideOnly.SERVER, the nested class is the class that is in error, not the top level class
                Side currentSide = Side.NONE;
                for (Pair<Side, PsiClass> pair : classHierarchyList) {
                    if (currentSide == Side.NONE) {
                        // If currentSide is NONE, then a class hasn't declared yet what it is
                        if (pair.first != Side.NONE && pair.first != Side.INVALID) {
                            currentSide = pair.first;
                        } else {
                            // We are only worried about this class
                            return;
                        }
                    } else if (pair.first != Side.NONE && pair.first != Side.INVALID) {
                        if (pair.first != currentSide) {
                            registerClassError(aClass, aClass);
                        } else {
                            return;
                        }
                    }
                }
            }
        };
    }
}
