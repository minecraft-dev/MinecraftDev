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
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.demonwav.mcdev.util.equivalentTo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

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
                        val description = describe(evaluated)
                        val otherDescription = describeOther(other)
                        holder.registerProblem(
                            constant,
                            if (description == null) {
                                MCDevBundle(
                                    "inspection.duplicate_unpick_constant.message",
                                    value.toDisplayString(),
                                    otherDescription,
                                )
                            } else {
                                MCDevBundle(
                                    "inspection.duplicate_unpick_constant.wildcard_message",
                                    description,
                                    value.toDisplayString(),
                                    otherDescription,
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
             * Describes the field a wildcard was expanded to, or returns `null` if this isn't a wildcard constant.
             */
            private fun describe(constant: EvaluatedConstant): String? {
                val field = constant.wildcardField ?: return null
                return "${field.containingClass?.name}.${field.name}"
            }

            private fun describeOther(constant: EvaluatedConstant) = describe(constant) ?: constant.constant.text

            private fun scopeKey(definition: UnpickGroupDefinition): Set<String> {
                return definition.scopes.mapTo(HashSet()) { scope -> scope.text.split(Regex("\\s+")).joinToString(" ") }
            }
        }
}
