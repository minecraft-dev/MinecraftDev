/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
    }
)

private object PropertyKeyCondition : PatternCondition<JsonElement>("isPropertyKey") {
    override fun accepts(t: JsonElement, context: ProcessingContext?) = JsonPsiUtil.isPropertyKey(t)
}
