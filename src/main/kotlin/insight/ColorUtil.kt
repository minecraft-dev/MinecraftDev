/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("UseJBColor")
package com.demonwav.mcdev.insight

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JVMElementFactories
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Color
import java.util.Locale
import kotlin.math.round
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UField
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.generate.generationPlugin
import org.jetbrains.uast.generate.replace
import org.jetbrains.uast.resolveToUElement

fun <T> UIdentifier.findColor(function: (Map<String, Color>, Map.Entry<String, Color>) -> T): T? {
    return runCatchingKtIdeaExceptions {
        val parent = this.uastParent
        val expression = parent as? UReferenceExpression ?: return null
        findColorFromExpression(expression, function)
    }
}

private fun <T> findColorFromExpression(
    expression: UReferenceExpression,
    function: (Map<String, Color>, Map.Entry<String, Color>) -> T,
    maxDepth: Int = 10,
    depth: Int = 0,
): T? {
    if (depth >= maxDepth) {
        return null
    }

    val referencedElement = runCatchingKtIdeaExceptions { expression.resolveToUElement() }
    if (referencedElement is UField) {
        val referencedFieldInitializer = referencedElement.uastInitializer
        if (referencedFieldInitializer is UReferenceExpression) {
            return findColorFromExpression(referencedFieldInitializer, function, maxDepth, depth + 1)
        }
    }

    val type = expression.getExpressionType() ?: return null
    val module = expression.sourcePsi?.findModule() ?: return null
    val facet = MinecraftFacet.getInstance(module) ?: return null
    val resolvedName = expression.resolvedName ?: return null
    for (abstractModuleType in facet.types) {
        val map = abstractModuleType.classToColorMappings(module)
        for (entry in map.entries) {
            // This is such a hack
            // Okay, type will be the fully-qualified class, but it will exclude the actual enum
            // the expression will be the non-fully-qualified class with the enum
            // So we combine those checks and get this
            val colorClass = entry.key.substringBeforeLast('.')
            val colorName = entry.key.substringAfterLast('.')
            if (colorClass.startsWith(type.canonicalText) && colorName == resolvedName) {
                return function(map.filterKeys { key -> key.startsWith(colorClass) }, entry)
            }
        }
    }
    return null
}

fun UIdentifier.findColor(
    moduleType: AbstractModuleType<AbstractModule>,
    className: String,
    vectorClasses: Array<String>?,
    maxDepth: Int = 10,
    depth: Int = 0,
): Pair<Color, UElement>? {
    if (depth >= maxDepth) {
        return null
    }

    val sourcePsi = this.sourcePsi ?: return null
    val module = ModuleUtilCore.findModuleForPsiElement(sourcePsi) ?: return null
    val facet = MinecraftFacet.getInstance(module) ?: return null
    if (!facet.isOfType(moduleType)) {
        return null
    }

    val methodExpression = uastParent as? UCallExpression
    if (methodExpression?.resolve()?.containingClass?.qualifiedName == className) {
        return findColorFromCallExpression(methodExpression, vectorClasses)
    }

    var referencedElement = runCatchingKtIdeaExceptions { (uastParent as? UResolvable)?.resolveToUElement() }
    while (referencedElement is UField) {
        val referencedFieldInitializer: UExpression? = referencedElement.uastInitializer
        if (referencedFieldInitializer is UCallExpression) {
            // The field is initialized with a method call
            val calledMethodIdentifier = referencedFieldInitializer.methodIdentifier ?: return null
            return calledMethodIdentifier.findColor(moduleType, className, vectorClasses, maxDepth, depth + 1)
        }

        if (referencedFieldInitializer is UReferenceExpression) {
            // The field is probably initialized with a reference to another field
            val referenceNameElement = referencedFieldInitializer.referenceNameElement
            if (referenceNameElement is UIdentifier) {
                // The expression was simple enough
                return referenceNameElement.findColor(moduleType, className, vectorClasses, maxDepth, depth + 1)
            } else if (referenceNameElement is UResolvable) {
                // The expression is complex, so we resolve it. If it is a field we're on for another round
                referencedElement = runCatchingKtIdeaExceptions { referenceNameElement.resolveToUElement() }
                continue
            }
        }

        break
    }

    return null
}

private fun findColorFromCallExpression(
    methodExpression: UCallExpression,
    vectorClasses: Array<String>?,
): Pair<Color, UElement>? {
    val project = methodExpression.sourcePsi?.project ?: return null

    val arguments = methodExpression.valueArguments
    val types = arguments.map(UExpression::getExpressionType)

    return when {
        // Single Integer Argument
        types.size == 1 ->
            colorFromSingleArgument(arguments[0])?.let { it to arguments[0] }
        // Triple Integer Argument
        types.size == 3 && types.all { it == PsiTypes.intType() } ->
            colorFromThreeArguments(arguments)?.let { it to methodExpression }
        vectorClasses != null && types.size == 1 -> {
            val scope = GlobalSearchScope.allScope(project)
            if (vectorClasses.any { types[0] == PsiType.getTypeByName(it, project, scope) }) {
                (arguments[0] as? UCallExpression)?.takeIf { it.valueArgumentCount == 3 }
                    ?.let(::colorFromVectorArgument)
                    ?.let { it to arguments[0] }
            } else {
                null
            }
        }
        else -> null
    }
}

private fun colorFromSingleArgument(expression: UExpression): Color? {
    return when (val paramVal = expression.evaluate()) {
        is Int -> Color(paramVal as? Int ?: return null)
        is String -> {
            if (paramVal.startsWith("#")) {
                val hexString = paramVal.substring(1)
                when (hexString.length) {
                    6 -> hexString.toIntOrNull(16)?.let(::Color)
                    3 -> {
                        val hexInt = hexString.toIntOrNull(16)
                            ?: return null
                        val r = (hexInt and 0xf00) shr 8 or (hexInt and 0xf00) shr 4
                        val g = (hexInt and 0x0f0) shr 4 or (hexInt and 0x0f0)
                        val b = (hexInt and 0x00f) shl 4 or (hexInt and 0x00f)
                        Color(r, g, b)
                    }
                    else -> null
                }
            } else {
                null
            }
        }
        else -> null
    }
}

private fun colorFromThreeArguments(expressions: List<UExpression>): Color? {
    fun normalize(value: Any?): Int? = when (value) {
        is Int -> value
        is Float -> round(value).toInt()
        is Double -> round(value).toInt()
        else -> null
    }

    val r = normalize(expressions[0].evaluate()) ?: return null
    val g = normalize(expressions[1].evaluate()) ?: return null
    val b = normalize(expressions[2].evaluate()) ?: return null
    return try {
        Color(r, g, b)
    } catch (e: IllegalArgumentException) {
        // Invalid color component
        null
    }
}

private fun colorFromVectorArgument(newExpression: UCallExpression): Color? {
    return colorFromThreeArguments(newExpression.valueArguments)
}

fun UElement.setColor(color: String, isStringLiteral: Boolean = false) {
    val sourcePsi = this.sourcePsi ?: return
    sourcePsi.containingFile.runWriteAction {
        val project = sourcePsi.project
        if (isStringLiteral) {
            val literal = generationPlugin?.getElementFactory(project)?.createStringLiteralExpression(color, sourcePsi)
                ?: return@runWriteAction
            this.replace(literal)
            return@runWriteAction
        }

        val parent = this.uastParent
        val newColorRef = generationPlugin?.getElementFactory(project)?.createQualifiedReference(color, sourcePsi)
            ?: return@runWriteAction
        if (this.lang.id == "kotlin") {
            // Kotlin UAST is a bit different, annoying but I couldn't find a better way
            val grandparent = parent?.uastParent
            if (grandparent is UQualifiedReferenceExpression) {
                grandparent.replace(newColorRef)
            } else {
                this.replace(newColorRef)
            }
        } else {
            if (parent is UQualifiedReferenceExpression) {
                parent.replace(newColorRef)
            } else {
                this.replace(newColorRef)
            }
        }
    }
}

fun ULiteralExpression.setColor(value: Int) {
    val sourcePsi = this.sourcePsi ?: return
    sourcePsi.containingFile.runWriteAction {
        JVMElementFactories.requireFactory(sourcePsi.language, sourcePsi.project)
            .createExpressionFromText("0x" + Integer.toHexString(value).uppercase(Locale.ENGLISH), sourcePsi)
            .let(sourcePsi::replace)
    }
}

fun UCallExpression.setColor(red: Int, green: Int, blue: Int) {
    val sourcePsi = this.sourcePsi ?: return
    sourcePsi.containingFile.runWriteAction {
        val r = this.valueArguments[0]
        val g = this.valueArguments[1]
        val b = this.valueArguments[2]

        val factory = JVMElementFactories.requireFactory(sourcePsi.language, sourcePsi.project)

        val literalExpressionOne = factory.createExpressionFromText(red.toString(), null)
        val literalExpressionTwo = factory.createExpressionFromText(green.toString(), null)
        val literalExpressionThree = factory.createExpressionFromText(blue.toString(), null)

        r.sourcePsi?.replace(literalExpressionOne)
        g.sourcePsi?.replace(literalExpressionTwo)
        b.sourcePsi?.replace(literalExpressionThree)
    }
}

fun UCallExpression.setColorHSV(h: Float, s: Float, v: Float) {
    val sourcePsi = this.sourcePsi ?: return
    sourcePsi.containingFile.runWriteAction {
        val hExpr = this.valueArguments[0]
        val sExpr = this.valueArguments[1]
        val vExpr = this.valueArguments[2]

        val factory = JVMElementFactories.requireFactory(sourcePsi.language, sourcePsi.project)

        val literalExpressionOne = factory.createExpressionFromText(h.toString() + "f", null)
        val literalExpressionTwo = factory.createExpressionFromText(s.toString() + "f", null)
        val literalExpressionThree = factory.createExpressionFromText(v.toString() + "f", null)

        hExpr.sourcePsi?.replace(literalExpressionOne)
        sExpr.sourcePsi?.replace(literalExpressionTwo)
        vExpr.sourcePsi?.replace(literalExpressionThree)
    }
}
