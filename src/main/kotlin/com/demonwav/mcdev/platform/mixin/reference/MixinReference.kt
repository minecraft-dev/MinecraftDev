/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference

interface MixinReference : PsiReference {

    enum class State {
        VALID, AMBIGUOUS, UNRESOLVED
    }

    val description: String

    val validate
        get() = if (resolve() != null) State.VALID else State.UNRESOLVED

    interface Poly : MixinReference, PsiPolyVariantReference {

        override val validate
            get() = when (multiResolve(false).size) {
                0 -> State.UNRESOLVED
                1 -> State.VALID
                else -> State.AMBIGUOUS
            }

    }

}
