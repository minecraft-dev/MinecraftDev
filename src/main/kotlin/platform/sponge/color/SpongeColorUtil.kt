/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.color

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Color
import kotlin.math.roundToInt

fun PsiElement.findColor(): Pair<Color, PsiElement>? {
    if (this !is PsiIdentifier) {
        return null
    }

    val project = this.getProject()

    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return null

    val facet = MinecraftFacet.getInstance(module) ?: return null

    if (!facet.isOfType(SpongeModuleType)) {
        return null
    }

    val methodExpression = this.parent as? PsiReferenceExpression ?: return null
    val qualifier = methodExpression.qualifier as? PsiReferenceExpression ?: return null
    if (qualifier.qualifiedName != "org.spongepowered.api.util.Color") {
        return null
    }

    val methodCallExpression = methodExpression.parent as? PsiMethodCallExpression ?: return null
    val expressionList = methodCallExpression.argumentList
    val types = expressionList.expressionTypes

    when {
        // Single Integer Argument
        types.size == 1 && types[0] == PsiType.INT && expressionList.expressions[0] is PsiLiteralExpression -> {
            try {
                val expr = expressionList.expressions[0] as PsiLiteralExpression
                return handleSingleArgument(expr) to expressionList.expressions[0]
            } catch (ignored: Exception) {}
        }
        // Triple Integer Argument
        types.size == 3 && types[0] == PsiType.INT && types[1] == PsiType.INT && types[2] == PsiType.INT -> {
            try {
                return handleThreeArguments(expressionList) to expressionList
            } catch (ignored: Exception) {}
        }
        // Single Vector3* Argument
        types.size == 1 -> {
            val scope = GlobalSearchScope.allScope(project)
            when (types[0]) {
                PsiType.getTypeByName("com.flowpowered.math.vector.Vector3i", project, scope),
                PsiType.getTypeByName("com.flowpowered.math.vector.Vector3f", project, scope),
                PsiType.getTypeByName("com.flowpowered.math.vector.Vector3d", project, scope) -> {
                    try {
                        val color = handleVectorArgument(expressionList.expressions[0] as PsiNewExpression)
                        return color to expressionList.expressions[0]
                    } catch (ignored: Exception) {}
                }
            }
        }
    }

    return null
}

private fun handleSingleArgument(expression: PsiLiteralExpression): Color {
    val value = Integer.decode(expression.text)

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

    val one = expressionOne.text.toDouble().roundToInt()
    val two = expressionTwo.text.toDouble().roundToInt()
    val three = expressionThree.text.toDouble().roundToInt()

    return Color(one, two, three)
}

private fun handleVectorArgument(newExpression: PsiNewExpression): Color {
    val expressionList =
        newExpression.node.findChildByType(JavaElementType.EXPRESSION_LIST) as PsiExpressionList? ?: throw Exception()

    return handleThreeArguments(expressionList)
}
