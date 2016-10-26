/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.color;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public class SpongeColorUtil {

    @Nullable
    public static Pair<Color, PsiElement> findColorFromElement(@NotNull PsiElement element) {
        if (!(element instanceof PsiMethodCallExpression)) {
            return null;
        }

        final Project project = element.getProject();

        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }

        final MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule == null) {
            return null;
        }

        if (!minecraftModule.isOfType(SpongeModuleType.getInstance())) {
            return null;
        }

        final PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) element;

        if (!(methodCallExpression.getMethodExpression().getQualifier() instanceof PsiReferenceExpression)) {
            return null;
        }

        final PsiReferenceExpression qualifier = (PsiReferenceExpression) methodCallExpression.getMethodExpression().getQualifier();
        if (qualifier == null) {
            return null;
        }

        if (!qualifier.getQualifiedName().equals("org.spongepowered.api.util.Color")) {
            return null;
        }

        final PsiExpressionList expressionList = methodCallExpression.getArgumentList();
        final PsiType[] types = expressionList.getExpressionTypes();

        Pair<Color, PsiElement> pair = null;

        // Single Integer Argument
        if (types.length == 1 && types[0] == PsiType.INT && expressionList.getExpressions()[0] instanceof PsiLiteralExpression) {
            pair = new Pair<>(
                handleSingleArgument((PsiLiteralExpression) expressionList.getExpressions()[0]),
                expressionList.getExpressions()[0]
            );


        // Triple Integer Argument
        } else if (types.length == 3 && types[0] == PsiType.INT && types[1] == PsiType.INT && types[2] == PsiType.INT) {
            pair = new Pair<>(
                handleThreeArguments(expressionList),
                expressionList
            );

        // Single Vector3* Argument
        } else if (types.length == 1 &&
            (
                types[0].equals(PsiType.getTypeByName("com.flowpowered.math.vector.Vector3i", project, GlobalSearchScope.allScope(project))) ||
                types[0].equals(PsiType.getTypeByName("com.flowpowered.math.vector.Vector3f", project, GlobalSearchScope.allScope(project))) ||
                types[0].equals(PsiType.getTypeByName("com.flowpowered.math.vector.Vector3d", project, GlobalSearchScope.allScope(project)))
            )
        ) {
            try {
                pair = new Pair<>(
                    handleVectorArgument((PsiNewExpression) expressionList.getExpressions()[0]),
                    expressionList.getExpressions()[0]
                );
            } catch (Exception ignored) {}
        }

        return pair;
    }

    @NotNull
    private static Color handleSingleArgument(@NotNull PsiLiteralExpression expression) {
        int value = Integer.decode(expression.getText());

        return new Color(value);
    }

    @Nullable
    private static Color handleThreeArguments(@NotNull PsiExpressionList expressionList) {
        if (!(expressionList.getExpressions()[0] instanceof PsiLiteralExpression) ||
            !(expressionList.getExpressions()[1] instanceof PsiLiteralExpression) ||
            !(expressionList.getExpressions()[2] instanceof PsiLiteralExpression)) {
            return null;
        }

        try {
            final PsiLiteralExpression expressionOne = (PsiLiteralExpression) expressionList.getExpressions()[0];
            final PsiLiteralExpression expressionTwo= (PsiLiteralExpression) expressionList.getExpressions()[1];
            final PsiLiteralExpression expressionThree = (PsiLiteralExpression) expressionList.getExpressions()[2];

            final int one = (int) Math.round(Double.parseDouble(expressionOne.getText()));
            final int two = (int) Math.round(Double.parseDouble(expressionTwo.getText()));
            final int three = (int) Math.round(Double.parseDouble(expressionThree.getText()));

            return new Color(one, two, three);
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static Color handleVectorArgument(@NotNull PsiNewExpression newExpression) {
        final PsiExpressionList expressionList = (PsiExpressionList) newExpression.getNode().findChildByType(JavaElementType.EXPRESSION_LIST);
        if (expressionList == null) {
            return null;
        }

        return handleThreeArguments(expressionList);
    }
}
