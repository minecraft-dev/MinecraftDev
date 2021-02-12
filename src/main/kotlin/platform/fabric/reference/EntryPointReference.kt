/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.util.manipulator
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ProcessingContext

object EntryPointReference : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is JsonStringLiteral) {
            return PsiReference.EMPTY_ARRAY
        }
        val manipulator = element.manipulator ?: return PsiReference.EMPTY_ARRAY
        val range = manipulator.getRangeInElement(element)
        val text = element.text.substring(range.startOffset, range.endOffset)
        val parts = text.split("::", limit = 2)
        return if (parts.size == 1) {
            arrayOf(Reference(element, range, false))
        } else {
            arrayOf(
                Reference(element, range.cutOut(TextRange.from(0, parts[0].length)), false),
                Reference(element, range.cutOut(TextRange.from(parts[0].length + 2, parts[1].length)), true)
            )
        }
    }

    private fun resolveReference(element: JsonStringLiteral, isMethodReference: Boolean): Array<PsiElement> {
        val strReference = element.value
        val parts = strReference.split("::", limit = 2)
        val clazz = JavaPsiFacade.getInstance(element.project).findClass(parts[0], element.resolveScope)
            ?: return PsiElement.EMPTY_ARRAY
        return if (isMethodReference) {
            if (parts.size == 1) {
                throw IncorrectOperationException("Invalid reference")
            }
            clazz.methods.filter { method ->
                method.name == parts[1] &&
                    method.hasModifierProperty(PsiModifier.PUBLIC) &&
                    method.hasModifierProperty(PsiModifier.STATIC)
            }.toTypedArray()
        } else {
            arrayOf(clazz)
        }
    }

    fun isEntryPointReference(reference: PsiReference) = reference is Reference

    private class Reference(element: JsonStringLiteral, range: TextRange, private val isMethodReference: Boolean) :
        PsiReferenceBase<JsonStringLiteral>(element, range),
        PsiPolyVariantReference,
        InspectionReference {

        override val description = "entry point '%s'"
        override val unresolved = resolve() == null

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return resolveReference(element, isMethodReference).map { PsiElementResolveResult(it) }.toTypedArray()
        }

        override fun resolve(): PsiElement? {
            val results = multiResolve(false)
            return if (results.size == 1) {
                results[0].element
            } else {
                null
            }
        }

        override fun bindToElement(newTarget: PsiElement): PsiElement? {
            val manipulator = element.manipulator ?: return null

            val range = manipulator.getRangeInElement(element)
            val text = element.text.substring(range.startOffset, range.endOffset)
            val parts = text.split("::", limit = 2)

            if (isMethodReference) {
                val targetMethod = newTarget as? PsiMethod
                    ?: throw IncorrectOperationException("Cannot target $newTarget")
                if (parts.size == 1) {
                    throw IncorrectOperationException("Invalid reference")
                }
                val methodRange = range.cutOut(TextRange.from(parts[0].length + 2, parts[1].length))
                return manipulator.handleContentChange(element, methodRange, targetMethod.name)
            } else {
                val targetClass = newTarget as? PsiClass
                    ?: throw IncorrectOperationException("Cannot target $newTarget")
                val classRange = if (parts.size == 1) {
                    range
                } else {
                    range.cutOut(TextRange.from(0, parts[0].length))
                }
                return manipulator.handleContentChange(element, classRange, targetClass.qualifiedName)
            }
        }
    }
}
