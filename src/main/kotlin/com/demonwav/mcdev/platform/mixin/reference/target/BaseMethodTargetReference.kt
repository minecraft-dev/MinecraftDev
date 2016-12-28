/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.util.getQualifiedInternalNameAndDescriptor
import com.demonwav.mcdev.util.internalName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor

internal abstract class BaseMethodTargetReference(element: PsiLiteral, methodReference: MixinReference)
    : QualifiedTargetReference<PsiMethod>(element, methodReference) {

    override fun createLookup(targetClass: PsiClass, m: PsiMethod, qualifier: PsiClassType?): LookupElementBuilder {
        return JavaLookupElementBuilder.forMethod(m, m.getQualifiedInternalNameAndDescriptor(qualifier),
                PsiSubstitutor.EMPTY, targetClass)
                .withPresentableText(m.internalName) // Display internal name (e.g. <init> for constructors)
                .withLookupString(m.internalName) // Allow looking up targets by their method name
    }

}
