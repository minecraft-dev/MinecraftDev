package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.PsiUtil;
import com.demonwav.mcdev.util.Util;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VariableSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of variable annotated with @SideOnly";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        int i = (int) infos[2];
        boolean inherited = (boolean) infos[4];

        if (i == 0) {
            if (infos[1] == null) {
                if (!inherited) {
                    return "Variable annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
                } else {
                    return "Variable declared in a class annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
                }
            } else {
                if (!inherited) {
                    return "Variable annotated with " + infos[0] + " cannot be referenced in a method annotated with " + infos[1] + ".";
                } else {
                    return "Variable declared in a class annotated with " + infos[0] +
                        " cannot be referenced in a method annotated with " + infos[1] + ".";
                }
            }
        } else {
            if (!inherited) {
                return "Variable annotated with " + infos[0] + " cannot be referenced in a class annotated with " + infos[1] + ".";
            } else {
                return "Variable declared in a class annotated with " + infos[0] +
                    " cannot be referenced in a class annotated with " + infos[1] + ".";
            }
        }
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Variables which are declared with a @SideOnly annotation can only be used in matching @SideOnly classes and methods.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        if (!(boolean) infos[4]) {
            return new InspectionGadgetsFix() {
                @Override
                protected void doFix(Project project, ProblemDescriptor descriptor) {
                    PsiFieldImpl field = (PsiFieldImpl) infos[3];

                    PsiModifierList list = field.getModifierList();

                    PsiAnnotation annotation = list.findAnnotation(SideOnlyUtil.SIDE_ONLY);
                    if (annotation == null) {
                        return;
                    }

                    annotation.delete();
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Remove @SideOnly annotation from field declaration";
                }

                @Nls
                @NotNull
                @Override
                public String getFamilyName() {
                    return getName();
                }
            };
        }
        return null;
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return;
                }

                PsiElement declaration = expression.resolve();

                // We can't really do anything unless this is a PsiFieldImpl, which it should be, but to be safe,
                // check the type before we make the cast
                if (!(declaration instanceof PsiFieldImpl)) {
                    return;
                }

                PsiFieldImpl field = (PsiFieldImpl) declaration;
                Side elementSide = SideOnlyUtil.checkField(field);

                // Check the class(es) the element is declared in
                PsiClass declarationContainingClass = PsiUtil.getClassOfElement(declaration);
                if (declarationContainingClass == null) {
                    return;
                }

                List<Pair<Side, PsiClass>> declarationClassHierarchySides = SideOnlyUtil.checkClassHierarchy(declarationContainingClass);

                Side declarationClassSide = Side.NONE;

                for (Pair<Side, PsiClass> classHierarchySide : declarationClassHierarchySides) {
                    if (classHierarchySide.first != Side.NONE && classHierarchySide.first != Side.INVALID) {
                        declarationClassSide = classHierarchySide.first;
                        break;
                    }
                }

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
                PsiClass containingClass = PsiUtil.getClassOfElement(expression);
                if (containingClass == null) {
                    return;
                }

                List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(containingClass);

                boolean classAnnotated = false;

                for (Pair<Side, PsiClass> classHierarchySide : classHierarchySides) {
                    if (classHierarchySide.first != Side.NONE && classHierarchySide.first != Side.INVALID) {
                        if (classHierarchySide.first != elementSide) {
                            registerError(expression.getElement(), elementSide.getName(), classHierarchySide.first.getName(), 1, field, inherited);
                        }
                        classAnnotated = true;
                        break;
                    }
                }

                // Check the method the element is in
                Side methodSide = SideOnlyUtil.checkElementInMethod(expression);

                // Put error on for method
                if (elementSide != methodSide && methodSide != Side.INVALID) {
                    if (methodSide == Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            registerError(expression.getElement(), elementSide.getName(), null, 0, field, inherited);
                        }
                    } else {
                        registerError(expression.getElement(), elementSide.getName(), methodSide.getName(), 0, field, inherited);
                    }
                }
            }
        };
    }
}
