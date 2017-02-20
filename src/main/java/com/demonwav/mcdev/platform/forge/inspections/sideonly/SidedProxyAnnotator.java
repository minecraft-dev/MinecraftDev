/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
import com.google.common.base.Strings;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class SidedProxyAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiField)) {
            return;
        }

        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return;
        }

        final MinecraftModule instance = MinecraftModule.getInstance(module);
        if (instance == null) {
            return;
        }

        if (!instance.isOfType(ForgeModuleType.getInstance())) {
            return;
        }

        final PsiField field = (PsiField) element;

        check(field);
    }

    public static void check(@NotNull PsiField field) {
        final PsiModifierList modifierList = field.getModifierList();
        if (modifierList == null) {
            return;
        }

        final PsiAnnotation annotation = modifierList.findAnnotation(ForgeConstants.SIDED_PROXY_ANNOTATION);
        if (annotation == null) {
            return;
        }

        final PsiAnnotationMemberValue clientSide = annotation.findAttributeValue("clientSide");
        final PsiAnnotationMemberValue serverSide = annotation.findAttributeValue("serverSide");

        if (clientSide != null && !Strings.isNullOrEmpty(clientSide.getText())) {
            annotateClass(clientSide, Side.CLIENT);
        }

        if (serverSide != null && !Strings.isNullOrEmpty(serverSide.getText())) {
            annotateClass(serverSide, Side.SERVER);
        }
    }

    private static void annotateClass(@NotNull PsiAnnotationMemberValue value, @NotNull Side side) {
        final String text;
        if (value instanceof PsiLiteralExpressionImpl) {
            final PsiLiteralExpressionImpl expression = (PsiLiteralExpressionImpl) value;

            text = expression.getInnerText();
            if (text == null) {
                return;
            }
        } else if (value instanceof PsiReferenceExpression) {
            final PsiReferenceExpression expression = (PsiReferenceExpression) value;

            final PsiElement resolve = expression.resolve();

            if (!(resolve instanceof PsiField)) {
                return;
            }

            final PsiField field = (PsiField) resolve;

            final Object o = JavaConstantExpressionEvaluator.computeConstantExpression(field.getInitializer(), null, false);
            if (!(o instanceof String)) {
                return;
            }

            text = (String) o;
        } else {
            return;
        }

        final PsiClass psiClass = JavaPsiFacade.getInstance(value.getProject()).findClass(text, GlobalSearchScope.allScope(value.getProject()));
        if (psiClass == null) {
            return;
        }

        psiClass.putUserData(Side.KEY, side);
    }
}
