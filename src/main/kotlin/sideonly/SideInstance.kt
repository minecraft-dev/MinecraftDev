/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.intellij.psi.PsiElement

class SideInstance private constructor(val side: Side, val element: PsiElement, val reason: String) {
    companion object {
        fun createSideOnly(side: Side, element: PsiElement): SideInstance {
            return SideInstance(side, element, "annotated with @SideOnly(Side.$side)")
        }
        fun createEnvironment(side: Side, element: PsiElement): SideInstance {
            return SideInstance(side, element, "annotated with @Environment(EnvType.$side)")
        }
        fun createOnlyIn(side: Side, element: PsiElement): SideInstance {
            return SideInstance(side, element, "annotated with @OnlyIn(Dist.${side.forgeName})")
        }
        fun createMcDev(side: Side, element: PsiElement): SideInstance {
            return SideInstance(side, element, "annotated with @CheckEnv(Env.$side)")
        }
        fun createImplicitMcDev(side: Side, element: PsiElement): SideInstance {
            return SideInstance(side, element, "implicitly annotated with @CheckEnv(Env.$side)")
        }

        fun createDistExecutor(side: Side, element: PsiElement): SideInstance {
            return SideInstance(side, element, "inside DistExecutor ${side.forgeName}")
        }
    }
}
