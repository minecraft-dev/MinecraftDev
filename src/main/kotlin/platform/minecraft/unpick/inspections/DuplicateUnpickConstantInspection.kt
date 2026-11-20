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
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickGroups
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator.EvaluatedConstant
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickValue
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.demonwav.mcdev.util.equivalentTo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class DuplicateUnpickConstantInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : UnpickVisitor() {
            override fun visitGroupDefinition(element: UnpickGroupDefinition) {
                val groupType = element.dataType?.dataType?.takeIf { it.isValidGroupType } ?: return
                val scopes = scopeKey(element)

                // Constants only conflict with constants in the same group with the same scope
                val definitions = UnpickGroups.findDefinitions(element, element.name)
                    .filter { it.dataType?.dataType == groupType && scopeKey(it) == scopes }
                    .ifEmpty { listOf(element) }

                val byValue = HashMap<UnpickValue, MutableList<EvaluatedConstant>>()
                for (definition in definitions) {
                    for (constant in definition.groupConstantList) {
                        for (evaluated in UnpickExpressionEvaluator.evaluate(constant).values) {
                            // Constants of the wrong type are reported by UnpickExpressionTypeInspection
                            if (!evaluated.value.type.canWidenTo(groupType)) {
                                continue
                            }
                            val value = evaluated.value.convertTo(groupType)
                            if (value.isKnown) {
                                byValue.getOrPut(value) { mutableListOf() } += evaluated
                            }
                        }
                    }
                }

                for (constant in element.groupConstantList) {
                    for (evaluated in UnpickExpressionEvaluator.evaluate(constant).values) {
                        if (!evaluated.value.type.canWidenTo(groupType)) {
                            continue
                        }
                        val value = evaluated.value.convertTo(groupType)
                        val other = byValue[value]?.firstOrNull { !it.isSameAs(evaluated) } ?: continue
                        val wildcardField = evaluated.wildcardField
                        holder.registerProblem(
                            constant,
                            if (wildcardField == null) {
                                MCDevBundle(
                                    "inspection.duplicate_unpick_constant.message",
                                    value.toDisplayString(),
                                    other.expandedText(),
                                )
                            } else {
                                MCDevBundle(
                                    "inspection.duplicate_unpick_constant.wildcard_message",
                                    value.toDisplayString(),
                                    "${wildcardField.containingClass?.name}.${wildcardField.name}",
                                    other.expandedText(),
                                )
                            },
                        )
                    }
                }
            }

            private fun EvaluatedConstant.isSameAs(other: EvaluatedConstant): Boolean {
                return constant equivalentTo other.constant && wildcardField == other.wildcardField
            }

            /**
             * Returns the text of the constant, with the wildcard replaced by the field it was expanded to.
             */
            private fun EvaluatedConstant.expandedText(): String {
                val field = wildcardField ?: return constant.text
                val wildcard = PsiTreeUtil.findChildrenOfType(constant, UnpickFieldExpression::class.java)
                    .firstNotNullOfOrNull { it.wildcardElement }
                    ?: return constant.text
                return wildcard.textRange.shiftLeft(constant.textRange.startOffset).replace(constant.text, field.name)
            }

            private fun scopeKey(definition: UnpickGroupDefinition): Set<String> {
                return definition.scopes.mapTo(HashSet()) { scope -> scope.text.split(Regex("\\s+")).joinToString(" ") }
            }
        }
}
