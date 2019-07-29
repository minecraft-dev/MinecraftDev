/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.METHOD_INJECTORS
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MixinReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Method references
        registrar.registerReferenceProvider(
            PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationAttribute(
                StandardPatterns.string().oneOf(METHOD_INJECTORS), "method"
            ), MethodReference
        )

        // Injection point types
        registrar.registerReferenceProvider(
            PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .insideAnnotationAttribute(AT), InjectionPointType
        )

        // Target references
        registrar.registerReferenceProvider(
            PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .insideAnnotationAttribute(AT, "target"), TargetReference
        )
    }
}
