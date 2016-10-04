package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNewExpression;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NewExpressionSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of class annotated with @SideOnly";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return "A class annotated with @SideOnly can only be used in other matching annotated classes and methods";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "A class that is annotated as @SideOnly(Side.CLIENT) or @SideOnly(Side.SERVER) cannot be used in classes or methods which " +
            "are annotated differently, or not at all. Since the irrelevant code is removed when operating as a server or a client, common " +
            "code cannot use @SideOnly annotated classes either.";
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
                    return "Remove @SideOnly annotation from class declaration";
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
            public void visitNewExpression(PsiNewExpression expression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return;
                }

                final PsiJavaCodeReferenceElement element = expression.getClassReference();
                if (element == null) {
                    return;
                }

                final PsiElement psiElement = element.resolve();
                if (psiElement == null) {
                    return;
                }

                if (!(psiElement instanceof PsiClass)) {
                    return;
                }

                final PsiClass psiClass = (PsiClass) psiElement;

                final List<Pair<Side, PsiClass>> list = SideOnlyUtil.checkClassHierarchy(psiClass);

                Side classSide = Side.NONE;

                PsiClass offender = null;
                for (Pair<Side, PsiClass> pair : list) {
                    if (pair.first != Side.NONE && pair.first != Side.INVALID) {
                        classSide = pair.first;
                        offender = pair.second;
                        break;
                    }
                }

                if (classSide == Side.NONE) {
                    return;
                }

                // Check the class(es) the element is in
                final PsiClass containingClass = McPsiUtil.getClassOfElement(expression);
                if (containingClass == null) {
                    return;
                }

                final Side containingClassSide = SideOnlyUtil.getSideForClass(containingClass);
                // Check the method the element is in
                final Side methodSide = SideOnlyUtil.checkElementInMethod(expression);

                boolean classAnnotated = false;

                if (containingClassSide != Side.NONE && containingClassSide != Side.INVALID) {
                    if (containingClassSide != classSide) {
                        registerError(expression, offender);
                    }
                    classAnnotated = true;
                } else {
                    if (methodSide == Side.INVALID) {
                        // It's not in a method
                        registerError(expression, offender);
                        return;
                    }
                }

                // Put error on for method
                if (classSide != methodSide && methodSide != Side.INVALID) {
                    if (methodSide == Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            registerError(expression, offender);
                        }
                    } else {
                        registerError(expression, offender);
                    }
                }
            }
        };
    }
}
