package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.PsiUtil;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.PsiFieldImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FieldDeclarationSideOnlyAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiFieldImpl)) {
            return;
        }

        if (SideOnlyUtil.beginningCheck(element)) {
            return;
        }

        PsiFieldImpl field = (PsiFieldImpl) element;

        PsiClass psiClass = PsiUtil.getClassOfElement(field);
        if (psiClass == null) {
            return;
        }

        Side fieldSide = SideOnlyUtil.checkField(field);

        List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(psiClass);
        Side classSide = SideOnlyUtil.getHighestLevelSide(classHierarchySides);

        if (fieldSide != classSide) {
            if (fieldSide != Side.NONE && classSide != Side.NONE && fieldSide != Side.INVALID && classSide != Side.INVALID) {
                holder.createErrorAnnotation(field.getNameIdentifier(), "Field annotated with " + fieldSide.getName() +
                    " cannot be declared inside a class annotated with " + classSide.getName() + ".");
            }
        }
    }
}
