/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.reference

import com.demonwav.mcdev.insight.uastEventListener
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.map2Array
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

class SpongeReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLanguageInjectionHost::class.java)
                .and(FilterPattern(GetterAnnotationFilter)),
            UastGetterEventListenerReferenceResolver,
            PsiReferenceRegistrar.HIGHER_PRIORITY
        )
    }
}

object UastGetterEventListenerReferenceResolver : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
        arrayOf(GetterReference(element as PsiLanguageInjectionHost))
}

private class GetterReference(element: PsiLanguageInjectionHost) : PsiReferenceBase<PsiLanguageInjectionHost>(element) {
    override fun resolve(): PsiElement? {
        val literal = element.toUElementOfType<ULiteralExpression>() ?: return null
        val targetName = literal.evaluateString() ?: return null

        val (eventClass, _) = literal.uastEventListener ?: return null
        return eventClass.javaPsi.findMethodsByName(targetName, true).firstOrNull(::isValidCandidate)
    }

    override fun getVariants(): Array<Any> {
        val literal = element.toUElementOfType<ULiteralExpression>() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val (eventClass, _) = literal.uastEventListener
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val methodByClass = mutableMapOf<String, Pair<PsiMethod, PsiClass>>()
        for (method in eventClass.javaPsi.allMethods) {
            if (!isValidCandidate(method)) {
                continue
            }

            val existingPair = methodByClass[method.name]
            if (existingPair == null || method.containingClass!!.isInheritor(existingPair.second, true)) {
                methodByClass[method.name] = method to method.containingClass!!
            }
        }
        return methodByClass.values.map2Array { JavaLookupElementBuilder.forMethod(it.first, PsiSubstitutor.EMPTY) }
    }
}

private object GetterAnnotationFilter : ElementFilter {
    override fun isAcceptable(element: Any, context: PsiElement?): Boolean {
        val type = context.toUElement() ?: return false
        val annotation = type.getParentOfType<UAnnotation>() ?: return false
        return annotation.qualifiedName == SpongeConstants.GETTER_ANNOTATION
    }

    override fun isClassAcceptable(hintClass: Class<*>): Boolean =
        PsiLanguageInjectionHost::class.java.isAssignableFrom(hintClass)
}

private fun isValidCandidate(method: PsiMethod): Boolean = method.returnType != PsiType.VOID &&
    !method.isConstructor && method.hasModifierProperty(PsiModifier.PUBLIC) && !method.hasParameters() &&
    method.containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT
