/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext

object EntryPointReference : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        return arrayOf(Reference(element as JsonStringLiteral))
    }

    private fun resolveReference(element: JsonStringLiteral): Array<PsiElement> {
        val strReference = element.value
        val parts = strReference.split("::", limit = 2)
        val clazz = JavaPsiFacade.getInstance(element.project).findClass(parts[0], element.resolveScope)
            ?: return PsiElement.EMPTY_ARRAY
        if (parts.size == 1) {
            return arrayOf(clazz)
        }
        return clazz.methods.filter { method ->
            method.name == parts[1] &&
                method.hasModifierProperty(PsiModifier.PUBLIC) &&
                method.hasModifierProperty(PsiModifier.STATIC)
        }.toTypedArray()
    }

    fun isEntryPointReference(reference: PsiReference) = reference is Reference

    private class Reference(element: JsonStringLiteral) :
        PsiReferenceBase<JsonStringLiteral>(element),
        PsiPolyVariantReference,
        InspectionReference {

        override val description = "entry point '%s'"
        override val unresolved = resolve() == null

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return resolveReference(element).map { PsiElementResolveResult(it) }.toTypedArray()
        }

        override fun resolve(): PsiElement? {
            val results = multiResolve(false)
            return if (results.size == 1) {
                results[0].element
            } else {
                null
            }
        }
    }
}
