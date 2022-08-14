/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.reflection.reference

import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class ReflectionReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .and(
                    PsiJavaPatterns.psiElement()
                        .methodCallParameter(PsiJavaPatterns.psiMethod().withName("findField"))
                ),
            ReflectedFieldReference
        )
        registrar.registerReferenceProvider(
            PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .and(
                    PsiJavaPatterns.psiElement()
                        .methodCallParameter(PsiJavaPatterns.psiMethod().withName("findMethod"))
                ),
            ReflectedMethodReference
        )
    }
}
