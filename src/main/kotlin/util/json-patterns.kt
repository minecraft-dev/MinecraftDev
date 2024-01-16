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

package com.demonwav.mcdev.util

import com.intellij.json.psi.JsonElement
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonPsiUtil
import com.intellij.json.psi.JsonValue
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PsiElementPattern
import com.intellij.util.ProcessingContext

fun PsiElementPattern.Capture<out JsonValue>.isPropertyKey() = with(PropertyKeyCondition)

fun PsiElementPattern.Capture<out JsonValue>.isPropertyValue(property: String) = with(
    object : PatternCondition<JsonElement>("isPropertyValue") {
        override fun accepts(t: JsonElement, context: ProcessingContext?): Boolean {
            val parent = t.parent as? JsonProperty ?: return false
            return parent.value == t && parent.name == property
        }
    },
)

private object PropertyKeyCondition : PatternCondition<JsonElement>("isPropertyKey") {
    override fun accepts(t: JsonElement, context: ProcessingContext?) = JsonPsiUtil.isPropertyKey(t)
}
