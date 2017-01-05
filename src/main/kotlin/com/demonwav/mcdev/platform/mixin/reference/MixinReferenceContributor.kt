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

import com.demonwav.mcdev.platform.mixin.reference.target.MixinTargetReferenceProvider
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INJECT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_ARG
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_CONSTANT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.REDIRECT
import com.demonwav.mcdev.util.insideAnnotationParam
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MixinReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injectorAnnotations = StandardPatterns.string().oneOf(INJECT, MODIFY_ARG, MODIFY_CONSTANT, MODIFY_VARIABLE, REDIRECT)

        // Method references
        registrar.registerReferenceProvider(PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .insideAnnotationParam(injectorAnnotations, "method"),
                MixinMethodReferenceProvider())

        // Injection point types
        registrar.registerReferenceProvider(PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .insideAnnotationParam(AT), MixinInjectionPointTypeReferenceProvider())

        // Target references
        registrar.registerReferenceProvider(PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .insideAnnotationParam(PsiJavaPatterns.psiAnnotation().qName(StandardPatterns.string().equalTo(AT))
                        .insideAnnotationAttribute("at", PsiJavaPatterns.psiAnnotation().qName(injectorAnnotations)), "target")
                , MixinTargetReferenceProvider())
    }

}
