/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.intellij.psi.PsiType

data class MethodSignature(val parameters: List<ParameterGroup>, val returnType: PsiType)
