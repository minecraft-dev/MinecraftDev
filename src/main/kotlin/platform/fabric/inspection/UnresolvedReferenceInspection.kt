/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.inspection

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

class UnresolvedReferenceInspection : LocalInspectionTool() {

    override fun getStaticDescription() = "Reports unresolved references in Fabric mod JSON files."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (holder.file.name == FabricConstants.FABRIC_MOD_JSON) {
            return Visitor(holder)
        }
        return PsiElementVisitor.EMPTY_VISITOR
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        if (file.name == FabricConstants.FABRIC_MOD_JSON) {
            return super.processFile(file, manager)
        }
        return emptyList()
    }

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
