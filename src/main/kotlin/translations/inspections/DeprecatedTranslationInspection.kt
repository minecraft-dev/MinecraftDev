/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.translations.inspections

import com.demonwav.mcdev.translations.DeprecatedTranslations
import com.demonwav.mcdev.translations.identification.TranslationIdentifier
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastHintedVisitorAdapter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class DeprecatedTranslationInspection : TranslationInspection() {
    private val typesHint: Array<Class<out UElement>> = arrayOf(UExpression::class.java)

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor =
        UastHintedVisitorAdapter.create(holder.file.language, Visitor(holder), typesHint)

    private class Visitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {
        val deprecations = DeprecatedTranslations.getInstance(holder.project)

        override fun visitExpression(node: UExpression): Boolean {
            val result = TranslationIdentifier.identify(node)
            if (result != null && result.text != null) {
                val isRemoved = result.key.full in deprecations.removed
                val isRenamed = result.key.full in deprecations.renamed
                if (isRemoved || isRenamed) {
                    val type = if (isRemoved) "removed" else "renamed"
                    holder.registerProblem(
                        node.sourcePsi!!,
                        "Usage of $type translation in deprecated.json"
                    )
                }
            }

            return super.visitExpression(node)
        }
    }
}
