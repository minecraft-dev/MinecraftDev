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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult

interface MixinReference : PsiReference {

    enum class State {
        VALID, AMBIGUOUS, UNRESOLVED
    }

    val description: String

    fun validate() = if (resolve() != null) State.VALID else State.UNRESOLVED
    fun resolveFirstIfValid() = resolve()

    interface Poly : MixinReference, PsiPolyVariantReference {

        override fun validate() = validate(multiResolve(false))

        fun validate(results: Array<ResolveResult>) = when (multiResolve(false).size) {
            0 -> State.UNRESOLVED
            1 -> State.VALID
            else -> State.AMBIGUOUS
        }

        override fun resolveFirstIfValid(): PsiElement? {
            val results = multiResolve(false)
            if (validate(results) == State.VALID) {
                return results.firstOrNull()?.element
            } else {
                return null
            }
        }
    }

}
