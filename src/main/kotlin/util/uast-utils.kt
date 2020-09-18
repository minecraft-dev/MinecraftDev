/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.toUElementOfType

fun UTypeReferenceExpression.resolve(): UClass? = (this.type as? PsiClassType)?.resolve().toUElementOfType()
