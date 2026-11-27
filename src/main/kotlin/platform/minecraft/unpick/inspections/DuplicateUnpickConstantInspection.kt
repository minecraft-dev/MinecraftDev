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

package com.demonwav.mcdev.platform.minecraft.unpick.inspections

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickConstantConflicts
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator.EvaluatedConstant
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElementFactory
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class DuplicateUnpickConstantInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : UnpickVisitor() {
            override fun visitGroupDefinition(element: UnpickGroupDefinition) {
                for (conflict in UnpickConstantConflicts.findConflicts(element)) {
                    val wildcardField = conflict.constant.wildcardField
                    holder.registerProblem(
                        conflict.constant.constant,
                        if (wildcardField == null) {
                            MCDevBundle(
                                "inspection.duplicate_unpick_constant.message",
                                conflict.value.toDisplayString(),
                                conflict.other.expandedText(),
                            )
                        } else {
                            MCDevBundle(
                                "inspection.duplicate_unpick_constant.wildcard_message",
                                conflict.value.toDisplayString(),
                                "${wildcardField.containingClass?.name}.${wildcardField.name}",
                                conflict.other.expandedText(),
                            )
                        },
                    )
                }
            }

            /**
             * Returns the text of the constant, with the wildcard replaced by the field it was expanded to.
             */
            private fun EvaluatedConstant.expandedText(): String {
                val field = wildcardField ?: return constant.text
                val copy = constant.copy()
                val wildcard = PsiTreeUtil.findChildrenOfType(copy, UnpickFieldExpression::class.java)
                    .firstNotNullOfOrNull { it.wildcardElement }
                    ?: return constant.text
                wildcard.replace(UnpickElementFactory.createFieldName(constant.project, field.name))
                return copy.text
            }
        }
}
