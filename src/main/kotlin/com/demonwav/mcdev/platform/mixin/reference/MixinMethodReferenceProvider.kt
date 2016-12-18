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

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.getClassOfElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

internal class MixinMethodReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val mixinClass = getClassOfElement(element) ?: return PsiReference.EMPTY_ARRAY
        val targets = MixinUtils.getAllMixedClasses(mixinClass).values

        return when (targets.size) {
            0 -> PsiReference.EMPTY_ARRAY
            1 -> arrayOf(MethodReferenceSingleTarget(element as PsiLiteral, targets.single()))
            else -> arrayOf(MethodReferenceMultipleTargets(element as PsiLiteral, targets))
        }
    }

}
