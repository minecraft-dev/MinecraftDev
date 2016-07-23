package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.PsiUtil;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MethodSideOnlyAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (SideOnlyUtil.beginningCheck(element)) {
            return;
        }

        // Only worried about methods here
        if (!(element instanceof PsiMethod)) {
            return;
        }

        PsiMethod method = (PsiMethod) element;

        Side methodSide = SideOnlyUtil.checkMethod(method);

        PsiClass psiClass = PsiUtil.getClassOfElement(method);
        if (psiClass == null) {
            return;
        }

        List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(psiClass);
        Side classSide = SideOnlyUtil.getHighestLevelSide(classHierarchySides);

        if (methodSide != classSide) {
            if (methodSide != Side.NONE && classSide != Side.NONE &&
                methodSide != Side.INVALID && classSide != Side.INVALID) {

                if (method.getNameIdentifier() != null) {
                    holder.createErrorAnnotation(method.getNameIdentifier(), "Method annotated with " + methodSide.getName() +
                        " cannot be declared inside a class annotated with " + classSide.getName() + ".");
                }
            }
        }
    }
}
