/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.reference

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.reference.ReferenceResolver
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil

object GetterEventListenerReferenceResolver : ReferenceResolver() {

    override fun resolveReference(context: PsiElement): PsiElement? {
        val method = context.findContainingMethod() ?: return null
        if (!method.hasParameters()) {
            return null
        }

        val eventType = method.parameters[0].type as? JvmReferenceType ?: return null

        val methodName = (context as? PsiLiteralExpression)?.value?.toString() ?: return null
        val methods = (eventType.resolve() as? PsiClass)?.findMethodsByName(methodName, true) ?: return null
        return methods.firstOrNull { !it.hasParameters() }
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val memberValue = context.parentOfType(PsiNameValuePair::class) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        if (memberValue.attributeName != "value") {
            return ArrayUtil.EMPTY_OBJECT_ARRAY
        }

        val annotation = memberValue.parentOfType(PsiAnnotation::class)
        if (annotation?.hasQualifiedName(SpongeConstants.GETTER_ANNOTATION) == false) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY
        }

        val eventHandler = context.findContainingMethod() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        if (!eventHandler.isValidSpongeListener()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY
        }

        val eventReferenceType = eventHandler.parameters[0].type as? JvmReferenceType
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val eventClass = eventReferenceType.resolve() as? PsiClass ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val methods = mutableListOf<LookupElement>()
        for (method in eventClass.allMethods) {
            if (method.returnType != PsiType.VOID && !method.hasParameters()
                && method.containingClass?.qualifiedName != "java.lang.Object"
            ) {
                methods += JavaLookupElementBuilder.forMethod(method, PsiSubstitutor.EMPTY)
                    .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
            }
        }

        return methods.toTypedArray()
    }
}
