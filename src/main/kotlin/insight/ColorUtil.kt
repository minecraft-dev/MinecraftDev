/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
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

fun <T> PsiElement.findColor(function: (Map<String, Color>, Map.Entry<String, Color>) -> T): T? {
    if (this !is PsiIdentifier) {
        return null
    }

    val expression = this.parent as? PsiReferenceExpression ?: return null
    val type = expression.type ?: return null

    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return null
    val facet = MinecraftFacet.getInstance(module) ?: return null
    for (abstractModuleType in facet.types) {
        val map = abstractModuleType.classToColorMappings
        for (entry in map.entries) {
            // This is such a hack
            // Okay, type will be the fully-qualified class, but it will exclude the actual enum
            // the expression will be the non-fully-qualified class with the enum
            // So we combine those checks and get this
            if (entry.key.startsWith(type.canonicalText) && entry.key.endsWith(expression.canonicalText)) {
                return function(map, entry)
            }
        }
    }
    return null
}

fun PsiElement.findColor(
    moduleType: AbstractModuleType<AbstractModule>,
    className: String,
    vectorClasses: Array<String>?
): Pair<Color, PsiElement>? {
    if (this !is PsiIdentifier) {
        return null
    }

    val project = this.getProject()

    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return null

    val facet = MinecraftFacet.getInstance(module) ?: return null

    if (!facet.isOfType(moduleType)) {
        return null
    }

    val methodExpression = this.parent as? PsiReferenceExpression ?: return null
    val qualifier = methodExpression.qualifier as? PsiReferenceExpression ?: return null
    if (qualifier.qualifiedName != className) {
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
                return colorFromSingleArgument(expr) to expressionList.expressions[0]
            } catch (ignored: Exception) { }
        }
        // Triple Integer Argument
        types.size == 3 && types[0] == PsiType.INT && types[1] == PsiType.INT && types[2] == PsiType.INT -> {
            try {
                return colorFromThreeArguments(expressionList) to expressionList
            } catch (ignored: Exception) { }
        }
        vectorClasses != null && types.size == 1 -> {
            val scope = GlobalSearchScope.allScope(project)
            for (vectorClass in vectorClasses) {
                if (types[0] == PsiType.getTypeByName(vectorClass, project, scope)) {
                    try {
                        val color = colorFromVectorArgument(expressionList.expressions[0] as PsiNewExpression)
                        return color to expressionList.expressions[0]
                    } catch (ignored: Exception) {}
                }
            }
        }
    }

    return null
}

fun PsiElement.setColor(color: String) {
    this.containingFile.runWriteAction {
        val split = color.split(".").dropLastWhile(String::isEmpty).toTypedArray()
        val newColorBase = split.last()

        val identifier = JavaPsiFacade.getElementFactory(this.project).createIdentifier(newColorBase)

        this.replace(identifier)
    }
}

fun PsiLiteralExpression.setColor(value: Int) {
    this.containingFile.runWriteAction {
        val node = this.node

        val literalExpression = JavaPsiFacade.getElementFactory(this.project)
            .createExpressionFromText("0x" + Integer.toHexString(value).toUpperCase(), null) as PsiLiteralExpression

        node.psi.replace(literalExpression)
    }
}

fun PsiExpressionList.setColor(red: Int, green: Int, blue: Int) {
    this.containingFile.runWriteAction {
        val expressionOne = this.expressions[0]
        val expressionTwo = this.expressions[1]
        val expressionThree = this.expressions[2]

        val nodeOne = expressionOne.node
        val nodeTwo = expressionTwo.node
        val nodeThree = expressionThree.node

        val facade = JavaPsiFacade.getElementFactory(this.project)

        val literalExpressionOne = facade.createExpressionFromText(red.toString(), null)
        val literalExpressionTwo = facade.createExpressionFromText(green.toString(), null)
        val literalExpressionThree = facade.createExpressionFromText(blue.toString(), null)

        nodeOne.psi.replace(literalExpressionOne)
        nodeTwo.psi.replace(literalExpressionTwo)
        nodeThree.psi.replace(literalExpressionThree)
    }
}

private fun colorFromSingleArgument(expression: PsiLiteralExpression): Color {
    val value = Integer.decode(expression.text)!!

    return Color(value)
}

private fun colorFromThreeArguments(expressionList: PsiExpressionList): Color {
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

private fun colorFromVectorArgument(newExpression: PsiNewExpression): Color {
    val expressionList =
        newExpression.node.findChildByType(JavaElementType.EXPRESSION_LIST) as PsiExpressionList? ?: throw Exception()

    return colorFromThreeArguments(expressionList)
}
