package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.PsiUtil;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiModifierList;
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
        return new InspectionGadgetsFix() {
            @Override
            protected void doFix(Project project, ProblemDescriptor descriptor) {
                PsiClass psiClass = (PsiClass) infos[3];

                PsiModifierList list = psiClass.getModifierList();
                if (list == null) {
                    return;
                }

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
                return "Remove @SideOnly annotation from class declaration";
            }

            @Nls
            @NotNull
            @Override
            public String getFamilyName() {
                return getName();
            }
        };
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitNewExpression(PsiNewExpression expression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return;
                }

                PsiJavaCodeReferenceElement element = expression.getClassReference();
                if (element == null) {
                    return;
                }

                PsiElement psiElement = element.resolve();
                if (psiElement == null) {
                    return;
                }

                if (!(psiElement instanceof PsiClass)) {
                    return;
                }

                PsiClass psiClass = (PsiClass) psiElement;

                List<Pair<Side, PsiClass>> list = SideOnlyUtil.checkClassHierarchy(psiClass);

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
                PsiClass containingClass = PsiUtil.getClassOfElement(expression);
                if (containingClass == null) {
                    return;
                }

                List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(containingClass);

                boolean classAnnotated = false;

                for (Pair<Side, PsiClass> classHierarchySide : classHierarchySides) {
                    if (classHierarchySide.first != Side.NONE && classHierarchySide.first != Side.INVALID) {
                        if (classHierarchySide.first != classSide) {
                            registerError(expression, classSide.getName(), classHierarchySide.first.getName(), 1, offender);
                        }
                        classAnnotated = true;
                        break;
                    }
                }

                // Check the method the element is in
                Side methodSide = SideOnlyUtil.checkElementInMethod(expression);

                // Put error on for method
                if (classSide != methodSide && methodSide != Side.INVALID) {
                    if (methodSide == Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            registerError(expression, classSide.getName(), null, 0, offender);
                        }
                    } else {
                        registerError(expression, classSide.getName(), methodSide.getName(), 0, offender);
                    }
                }
            }
        };
    }
}
