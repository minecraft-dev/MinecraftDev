package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.PsiUtil;
import com.demonwav.mcdev.util.Util;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.source.PsiFieldImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VariableSideOnlyAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // We are only checking PsiReferenceExpressions
        // We can get the PsiIdentifier from the PsiReferenceExpression later (which we do)
        // the PsiIdentifier is the actual element we are worried about, but we can only get
        // the declaration of the element from a PsiReferenceExpression, which wraps the
        // PsiIdentifier
        if (!(element instanceof PsiReferenceExpression)) {
            return;
        }

        if (SideOnlyUtil.beginningCheck(element)) {
            return;
        }

        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) element;

        new Thread(() -> ApplicationManager.getApplication().runReadAction(() -> {
            // This could be a long operation, which is why this is not run on the event dispatch thread
            PsiElement declaration = referenceExpression.resolve();

            // We can't really do anything unless this is a PsiFieldImpl, which it should be, but to be safe,
            // check the type before we make the cast
            if (!(declaration instanceof PsiFieldImpl)) {
                return;
            }

            PsiFieldImpl field = (PsiFieldImpl) declaration;
            Side elementSide = SideOnlyUtil.checkField(field);

            // Check the method the element is in
            Side methodSide = SideOnlyUtil.checkElementInMethod(element);
            // Put error on for method
            if (elementSide != methodSide && elementSide != Side.INVALID && methodSide != Side.INVALID) {
                if (methodSide == Side.NONE) {
                    Util.invokeLater(() ->
                        holder.createErrorAnnotation(referenceExpression.getElement(), "Variable annotated with " + elementSide.getName() +
                        " cannot be referenced in an un-annotated method.")
                    );
                } else {
                    Util.invokeLater(() ->
                        holder.createErrorAnnotation(referenceExpression.getElement(), "Variable annotated with " + elementSide.getName() +
                        " cannot be referenced in a method annotated with " + methodSide.getName() + ".")
                    );
                }
            }

            // Check the class(es) the element is in
            PsiClass containingClass = PsiUtil.getClassOfElement(element);
            if (containingClass == null) {
                return;
            }

            List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(containingClass);

            // Get the most relevant Side from the highest level class that declares
            Side elementRelevantClassSide = SideOnlyUtil.getHighestLevelSide(classHierarchySides);

            if (elementRelevantClassSide != Side.NONE && elementSide != Side.INVALID &&
                elementRelevantClassSide != Side.INVALID) {

                if (elementRelevantClassSide != elementSide) {
                    Util.invokeLater(() ->
                        holder.createErrorAnnotation(referenceExpression.getElement(), "Variable annotated with " + elementSide.getName() +
                        " cannot be referenced in a class annotated with " + elementRelevantClassSide.getName() + ".")
                    );
                }
            }
        })).start();
    }
}
