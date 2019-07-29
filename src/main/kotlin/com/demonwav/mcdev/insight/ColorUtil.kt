/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import java.awt.Color

fun <T> PsiElement.findColor(function: (Map<String, Color>, Map.Entry<String, Color>) -> T): T? {
    if (this !is PsiReferenceExpression) {
        return null
    }

    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return null

    val facet = MinecraftFacet.getInstance(module) ?: return null

    val expression = this
    val type = expression.type ?: return null

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

fun PsiElement.setColor(color: String) {
    this.containingFile.runWriteAction {
        val split = color.split(".").dropLastWhile(String::isEmpty).toTypedArray()
        val newColorBase = split.last()

        val node = this.node
        val child = node.findChildByType(JavaTokenType.IDENTIFIER) ?: return@runWriteAction

        val identifier = JavaPsiFacade.getElementFactory(this.project).createIdentifier(newColorBase)

        child.psi.replace(identifier)
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
