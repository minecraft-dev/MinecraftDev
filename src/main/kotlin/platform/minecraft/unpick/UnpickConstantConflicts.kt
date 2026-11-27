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


package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator.EvaluatedConstant
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickValue
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.index.UnpickGroupIndex
import com.demonwav.mcdev.util.equivalentTo
import com.intellij.lang.Language
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/**
 * Finds group constants that evaluate to the same value as another constant in the same group, where both can apply.
 */
object UnpickConstantConflicts {

    /**
     * [constant] and [other] both evaluate to [value], which is converted to the group type.
     */
    class Conflict(val constant: EvaluatedConstant, val value: UnpickValue, val other: EvaluatedConstant)

    /**
     * Returns a conflict for each constant of [definition] whose value is shared by another constant of the same group
     * and type. Definitions only conflict when both are unscoped or they have a scope in common. [Conflict.other] is
     * from the same file when possible.
     */
    fun findConflicts(definition: UnpickGroupDefinition): List<Conflict> {
        val groupType = groupType(definition) ?: return emptyList()
        val keys = UnpickGroupIndex.Util.conflictKeys(definition)
        val summaries = UnpickGroups.findFiles(definition, keys).map(::summary)

        val result = mutableListOf<Conflict>()
        for (constant in definition.groupConstantList) {
            for (evaluated in UnpickExpressionEvaluator.evaluate(constant).values) {
                val value = groupValue(evaluated, groupType) ?: continue
                val other = summaries.firstNotNullOfOrNull { summary ->
                    keys.firstNotNullOfOrNull { key ->
                        summary[key]?.get(value)?.firstOrNull { !it.isSameAs(evaluated) }
                    }
                } ?: continue
                result += Conflict(evaluated, value, other)
            }
        }
        return result
    }

    /**
     * The constants of a file, grouped by conflict key and then by value.
     */
    private fun summary(file: UnpickFile): Map<String, Map<UnpickValue, List<EvaluatedConstant>>> {
        return CachedValuesManager.getCachedValue(file) {
            // Constants only depend on their own file and on Java. Every PSI change counts towards Language.ANY, so it
            // has to be excluded for edits to other unpick files to keep this cached.
            val otherLanguages = PsiModificationTracker.getInstance(file.project)
                .forLanguages { it != Language.ANY && !it.isKindOf(UnpickLanguage) }
            CachedValueProvider.Result.create(buildSummary(file), file, otherLanguages)
        }
    }

    private fun buildSummary(file: UnpickFile): Map<String, Map<UnpickValue, List<EvaluatedConstant>>> {
        val summary = HashMap<String, HashMap<UnpickValue, MutableList<EvaluatedConstant>>>()
        for (definition in file.groupDefinitions) {
            val groupType = groupType(definition) ?: continue
            val keys = UnpickGroupIndex.Util.conflictKeys(definition)
            for (constant in definition.groupConstantList) {
                for (evaluated in UnpickExpressionEvaluator.evaluate(constant).values) {
                    val value = groupValue(evaluated, groupType) ?: continue
                    for (key in keys) {
                        summary.getOrPut(key, ::HashMap).getOrPut(value, ::ArrayList) += evaluated
                    }
                }
            }
        }
        return summary
    }

    private fun groupType(definition: UnpickGroupDefinition): DataType? {
        return definition.dataType?.dataType?.takeIf { it.isValidGroupType }
    }

    /**
     * Returns the value of the constant converted to the group type, or `null` if it's unknown or of the wrong type.
     */
    private fun groupValue(evaluated: EvaluatedConstant, groupType: DataType): UnpickValue? {
        // Constants of the wrong type are reported by UnpickExpressionTypeInspection
        if (!evaluated.value.type.canWidenTo(groupType)) {
            return null
        }
        return evaluated.value.convertTo(groupType).takeIf { it.isKnown }
    }

    private fun EvaluatedConstant.isSameAs(other: EvaluatedConstant): Boolean {
        return constant equivalentTo other.constant && wildcardField == other.wildcardField
    }
}
