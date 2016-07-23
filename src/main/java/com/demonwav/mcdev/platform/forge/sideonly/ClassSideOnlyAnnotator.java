package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.Util;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClassSideOnlyAnnotator implements Annotator{

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiClass)) {
            return;
        }

        if (!SideOnlyUtil.beginningCheck(element)) {
            return;
        }

        PsiClass psiClass = (PsiClass) element;

        List<Pair<Side, PsiClass>> classHierarchyList = SideOnlyUtil.checkClassHierarchy(psiClass);

        checkClassList(classHierarchyList, holder);
    }

    private void checkClassList(@NotNull List<Pair<Side, PsiClass>> list, @NotNull AnnotationHolder holder) {
        // The class lists are ordered from lowest to highest in the hierarchy - that is the first element in the list
        // is the most nested class, and the last element in the list is the top level class
        //
        // We first check that all the classes are correctly annotated, in both stacks
        // In this case, the higher-level classes take precedence, so if a class is annotated as @SideOnly.CLIENT and a nested class is
        // annotated as @SideOnly.SERVER, the nested class is the class that is in error, not the top level class
        //
        // Because of this, we iterate over the classes in reverse order
        Side currentSide = Side.NONE;
        for (int i = list.size() - 1; i >= 0; i--) {
            Pair<Side, PsiClass> pair = list.get(i);

            if (currentSide == Side.NONE) {
                // If currentSide is NONE, then a class hasn't declared yet what it is
                currentSide = pair.first;
            } else if ((pair.first != Side.NONE && pair.first != Side.INVALID) && pair.first != currentSide) {
                // If the currentSide is not NONE, and pair has declared a side, then they must be equal
                // If they aren't, then this class is in error
                Util.invokeLater(() ->
                    holder.createErrorAnnotation(pair.second, "A nested class cannot declare a side that is different from the parent class. " +
                        "Either remove the nested class's @SideOnly annotation, or change it to match it's parent's side.")
                );
            }
        }
    }
}
