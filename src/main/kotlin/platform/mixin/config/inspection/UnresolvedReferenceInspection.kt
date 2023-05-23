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

package com.demonwav.mcdev.platform.mixin.config.inspection

import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElementVisitor

class UnresolvedReferenceInspection : MixinConfigInspection() {

    override fun getStaticDescription() = "Reports unresolved references in Mixin configuration files."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JsonElementVisitor() {

        override fun visitStringLiteral(literal: JsonStringLiteral) {
            for (reference in literal.references) {
                if (reference !is InspectionReference) {
                    continue
                }

                if (reference.unresolved) {
                    holder.registerProblem(
                        literal,
                        "Cannot resolve ${reference.description}".format(reference.canonicalText),
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                        reference.rangeInElement,
                    )
                }
            }
        }
    }
}
