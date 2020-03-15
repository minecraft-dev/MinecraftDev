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

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType

data class Parameter(val name: String?, val type: PsiType) {
    constructor(parameter: PsiParameter) : this(parameter.name, parameter.type)
}
