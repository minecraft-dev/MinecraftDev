/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.intellij.psi.PsiParameter

data class ParameterGroup(val parameters: List<Parameter>?,
                          val required: Boolean = parameters != null,
                          val default: Boolean = required) {

    val size
        get() = this.parameters?.size ?: 0

    val wildcard
        get() = this.parameters == null

    fun match(parameters: Array<PsiParameter>, currentPosition: Int): Boolean {
        if (this.parameters == null) {
            // Wildcard parameter groups always match
            return true
        }

        // Check if remaining parameter count is enough
        if (currentPosition + size > parameters.size) {
            return false
        }

        var pos = currentPosition

        // Check parameter types
        for ((_, type) in this.parameters) {
            if (!type.isErasureEquivalentTo(parameters[pos++].type)) {
                return false
            }
        }

        return true
    }
}
