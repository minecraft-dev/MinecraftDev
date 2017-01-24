/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.signature

import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.intellij.psi.PsiParameter

internal data class ParameterGroup(internal val parameters: List<Parameter>?,
                                   internal val required: Boolean = parameters != null,
                                   internal val default: Boolean = required) {

    internal val size
        get() = this.parameters?.size ?: 0

    internal val wildcard
        get() = this.parameters == null

    internal fun match(parameters: Array<PsiParameter>, currentPosition: Int): Boolean {
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
