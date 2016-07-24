package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceExpression;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MethodCallSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of a @SideOnly method call";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        int i = (int) infos[2];
        boolean inherited = (boolean) infos[4];

        if (i == 0) {
            if (infos[1] == null) {
                if (!inherited) {
                    return "Method annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
                } else {
                    return "Method declared in a class annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
                }
            } else {
                if (!inherited) {
                    return "Method annotated with " + infos[0] + " cannot be referenced in a method annotated with " + infos[1] + ".";
                } else {
                    return "Method declared in a class annotated with " + infos[0] +
                        " cannot be referenced in a method annotated with " + infos[1] + ".";
                }
            }
        } else {
            if (!inherited) {
                return "Method annotated with " + infos[0] + " cannot be referenced in a class annotated with " + infos[1] + ".";
            } else {
                return "Method declared in a class annotated with " + infos[0] +
                    " cannot be referenced in a class annotated with " + infos[1] + ".";
            }
        }
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Methods which are declared with a @SideOnly annotation can only be used in matching @SideOnly classes and methods.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        PsiMethod method = (PsiMethod) infos[3];

        if (method.isWritable()) {
            return new RemoveAnnotationInspectionGadgetsFix() {
                @Nullable
                @Override
                public PsiModifierListOwner getListOwner() {
                    return method;
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Remove @SideOnly annotation from method declaration";
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
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return;
                }

                PsiReferenceExpression referenceExpression = expression.getMethodExpression();

                PsiElement declaration = referenceExpression.resolve();

                // We can't really do anything unless this is a PsiFieldImpl, which it should be, but to be safe,
                // check the type before we make the cast
                if (!(declaration instanceof PsiMethod)) {
                    return;
                }

                PsiMethod method = (PsiMethod) declaration;
                Side elementSide = SideOnlyUtil.checkMethod(method);

                // Check the class(es) the element is declared in
                PsiClass declarationContainingClass = McPsiUtil.getClassOfElement(declaration);
                if (declarationContainingClass == null) {
                    return;
                }

                List<Pair<Side, PsiClass>> declarationClassHierarchySides = SideOnlyUtil.checkClassHierarchy(declarationContainingClass);

                Side declarationClassSide = SideOnlyUtil.getFirstSide(declarationClassHierarchySides);

                // The element inherits the @SideOnly from it's parent class if it doesn't explicitly set it itself
                boolean inherited = false;
                if (declarationClassSide != Side.NONE && (elementSide == Side.INVALID || elementSide == Side.NONE)) {
                    inherited = true;
                    elementSide = declarationClassSide;
                }

                if (elementSide == Side.INVALID || elementSide == Side.NONE) {
                    return;
                }

                // Check the class(es) the element is in
                PsiClass containingClass = McPsiUtil.getClassOfElement(expression);
                if (containingClass == null) {
                    return;
                }

                Side classSide = SideOnlyUtil.getSideForClass(containingClass);

                boolean classAnnotated = false;

                if (classSide != Side.NONE && classSide != Side.INVALID) {
                    if (classSide != elementSide) {
                        registerError(referenceExpression.getElement(), elementSide.getName(), classSide.getName(), 1, method, inherited);
                    }
                    classAnnotated = true;
                }

                // Check the method the element is in
                Side methodSide = SideOnlyUtil.checkElementInMethod(expression);

                // Put error on for method
                if (elementSide != methodSide && methodSide != Side.INVALID) {
                    if (methodSide == Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            registerError(referenceExpression.getElement(), elementSide.getName(), null, 0, method, inherited);
                        }
                    } else {
                        registerError(referenceExpression.getElement(), elementSide.getName(), methodSide.getName(), 0, method, inherited);
                    }
                }
            }
        };
    }
}
