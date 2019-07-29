/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.color

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Color

fun PsiElement.findColor(): Pair<Color, PsiElement>? {
    if (this !is PsiMethodCallExpression) {
        return null
    }

    val project = this.getProject()

    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return null

    val facet = MinecraftFacet.getInstance(module) ?: return null

    if (!facet.isOfType(SpongeModuleType)) {
        return null
    }

    val methodCallExpression = this

    if (methodCallExpression.methodExpression.qualifier !is PsiReferenceExpression) {
        return null
    }

    val qualifier = methodCallExpression.methodExpression.qualifier as PsiReferenceExpression? ?: return null

    if (qualifier.qualifiedName != "org.spongepowered.api.util.Color") {
        return null
    }

    val expressionList = methodCallExpression.argumentList
    val types = expressionList.expressionTypes

    var pair: Pair<Color, PsiElement>? = null

    // Single Integer Argument
    if (types.size == 1 && types[0] === PsiType.INT && expressionList.expressions[0] is PsiLiteralExpression) {
        try {
            val expr = expressionList.expressions[0] as PsiLiteralExpression
            pair = handleSingleArgument(expr) to expressionList.expressions[0]
        } catch (ignored: Exception) {
        }

        // Triple Integer Argument
    } else if (types.size == 3 && types[0] === PsiType.INT && types[1] === PsiType.INT && types[2] === PsiType.INT) {
        try {
            pair = handleThreeArguments(expressionList) to expressionList
        } catch (ignored: Exception) {
        }

        // Single Vector3* Argument
    } else if (types.size == 1 && (
            types[0] == PsiType.getTypeByName(
                "com.flowpowered.math.vector.Vector3i",
                project,
                GlobalSearchScope.allScope(project)
            ) ||
                types[0] == PsiType.getTypeByName(
                "com.flowpowered.math.vector.Vector3f",
                project,
                GlobalSearchScope.allScope(project)
            ) ||
                types[0] == PsiType.getTypeByName(
                "com.flowpowered.math.vector.Vector3d",
                project,
                GlobalSearchScope.allScope(project)
            ))
    ) {

        try {
            pair =
                handleVectorArgument(expressionList.expressions[0] as PsiNewExpression) to expressionList.expressions[0]
        } catch (ignored: Exception) {
        }
    }

    return pair
}

private fun handleSingleArgument(expression: PsiLiteralExpression): Color {
    val value = Integer.decode(expression.text)!!

    return Color(value)
}

private fun handleThreeArguments(expressionList: PsiExpressionList): Color {
    if (expressionList.expressions[0] !is PsiLiteralExpression ||
        expressionList.expressions[1] !is PsiLiteralExpression ||
        expressionList.expressions[2] !is PsiLiteralExpression
    ) {
        throw Exception()
    }

    val expressionOne = expressionList.expressions[0] as PsiLiteralExpression
    val expressionTwo = expressionList.expressions[1] as PsiLiteralExpression
    val expressionThree = expressionList.expressions[2] as PsiLiteralExpression

    val one = Math.round(expressionOne.text.toDouble()).toInt()
    val two = Math.round(expressionTwo.text.toDouble()).toInt()
    val three = Math.round(expressionThree.text.toDouble()).toInt()

    return Color(one, two, three)
}

private fun handleVectorArgument(newExpression: PsiNewExpression): Color {
    val expressionList =
        newExpression.node.findChildByType(JavaElementType.EXPRESSION_LIST) as PsiExpressionList? ?: throw Exception()

    return handleThreeArguments(expressionList)
}
