/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import java.awt.Color;
import java.util.Map;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorUtil {

    @Nullable
    public static <T> T findColorFromElement(@NotNull PsiElement element,
                                             @NotNull BiFunction<Map<String, Color>, Map.Entry<String, Color>, T> function) {
        if (!(element instanceof PsiReferenceExpression)) {
            return null;
        }

        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }

        MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule == null) {
            return null;
        }

        PsiReferenceExpression expression = (PsiReferenceExpression) element;
        PsiType type = expression.getType();
        if (type == null) {
            return null;
        }

        for (AbstractModuleType<?> abstractModuleType : minecraftModule.getTypes()) {
            Map<String, Color> map = abstractModuleType.getClassToColorMappings();
            for (Map.Entry<String, Color> entry : map.entrySet()) {
                // This is such a hack
                // Okay, type will be the fully-qualified class, but it will exclude the actual enum
                // the expression will be the non-fully-qualified class with the enum
                // So we combine those checks and get this
                if (entry.getKey().startsWith(type.getCanonicalText()) &&
                        entry.getKey().endsWith(expression.getCanonicalText())) {
                    return function.apply(map, entry);
                }
            }
        }
        return null;
    }

    public static void setColorTo(@NotNull PsiElement element, @NotNull String color) {
        try {
            WriteCommandAction.runWriteCommandAction(element.getProject(), () -> {
                String[] split = color.split("\\.");
                String newColorBase = split[split.length - 1];

                ASTNode node = element.getNode();
                ASTNode child = node.findChildByType(JavaTokenType.IDENTIFIER);
                if (child == null) {
                    return;
                }

                PsiIdentifier identifier = JavaPsiFacade.getElementFactory(element.getProject()).createIdentifier(newColorBase);

                child.getPsi().replace(identifier);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void setColorTo(@NotNull PsiLiteralExpression expression, int value) {
        try {
            WriteCommandAction.runWriteCommandAction(expression.getProject(), () -> {
                ASTNode node = expression.getNode();

                PsiLiteralExpression literalExpression = (PsiLiteralExpression) JavaPsiFacade.getElementFactory(expression.getProject())
                        .createExpressionFromText("0x" + Integer.toHexString(value).toUpperCase(), null);

                node.getPsi().replace(literalExpression);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void setColorTo(@NotNull PsiExpressionList expressionList, int red, int green, int blue) {
        try {
            WriteCommandAction.runWriteCommandAction(expressionList.getProject(), () -> {
                PsiExpression expressionOne = expressionList.getExpressions()[0];
                PsiExpression expressionTwo = expressionList.getExpressions()[1];
                PsiExpression expressionThree = expressionList.getExpressions()[2];

                ASTNode nodeOne = expressionOne.getNode();
                ASTNode nodeTwo = expressionTwo.getNode();
                ASTNode nodeThree  = expressionThree.getNode();

                PsiExpression literalExpressionOne = JavaPsiFacade.getElementFactory(expressionList.getProject())
                        .createExpressionFromText(String.valueOf(red), null);
                PsiExpression literalExpressionTwo = JavaPsiFacade.getElementFactory(expressionList.getProject())
                        .createExpressionFromText(String.valueOf(green), null);
                PsiExpression literalExpressionThree = JavaPsiFacade.getElementFactory(expressionList.getProject())
                        .createExpressionFromText(String.valueOf(blue), null);

                nodeOne.getPsi().replace(literalExpressionOne);
                nodeTwo.getPsi().replace(literalExpressionTwo);
                nodeThree.getPsi().replace(literalExpressionThree);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
