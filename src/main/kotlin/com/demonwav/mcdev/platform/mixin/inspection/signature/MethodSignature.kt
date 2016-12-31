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

import com.intellij.psi.PsiType

internal data class MethodSignature(internal val parameters: List<ParameterGroup>, internal val returnType: PsiType)
