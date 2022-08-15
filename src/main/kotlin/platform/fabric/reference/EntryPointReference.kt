/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
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
        val methodParts = text.split("::", limit = 2)
        val clazzParts = methodParts[0].split("$", limit = 0)
        val references = mutableListOf<Reference>()
        var cursor = -1
        var innerClassDepth = -1
        for (clazzPart in clazzParts) {
            cursor++
            innerClassDepth++
            references.add(
                Reference(
                    element,
                    range.cutOut(TextRange.from(cursor, clazzPart.length)),
                    innerClassDepth,
                    false
                )
            )
            cursor += clazzPart.length
        }
        if (methodParts.size == 2) {
            cursor += 2
            references.add(
                Reference(
                    element,
                    range.cutOut(TextRange.from(cursor, methodParts[1].length)),
                    innerClassDepth,
                    true
                )
            )
        }
        return references.toTypedArray()
    }

    private fun resolveReference(
        element: JsonStringLiteral,
        innerClassDepth: Int,
        isMethodReference: Boolean
    ): Array<PsiElement> {
        val strReference = element.value
        val methodParts = strReference.split("::", limit = 2)
        // split at dollar sign for inner class evaluation
        val clazzParts = methodParts[0].split("$", limit = 0)
        // this case should only happen if someone misuses the method, better protect against it anyways
        if (innerClassDepth >= clazzParts.size ||
            innerClassDepth + 1 < clazzParts.size &&
            isMethodReference
        ) throw IncorrectOperationException("Invalid reference")
        var clazz = JavaPsiFacade.getInstance(element.project).findClass(clazzParts[0], element.resolveScope)
            ?: return PsiElement.EMPTY_ARRAY
        // if class is inner class, then a dot "." was used as separator instead of a dollar sign "$", this does not work to reference an inner class
        if (clazz.parent is PsiClass) return PsiElement.EMPTY_ARRAY
        // walk inner classes
        for (inner in clazzParts.drop(1).take(innerClassDepth)) {
            // we don't want any dots "." in the names of the inner classes
            if (inner.contains('.')) return PsiElement.EMPTY_ARRAY
            clazz = clazz.findInnerClassByName(inner, false) ?: return PsiElement.EMPTY_ARRAY
        }
        return if (isMethodReference) {
            if (methodParts.size == 1) {
                throw IncorrectOperationException("Invalid reference")
            }
            clazz.methods.filter { method ->
                method.name == methodParts[1] &&
                    method.hasModifierProperty(PsiModifier.PUBLIC) &&
                    method.hasModifierProperty(PsiModifier.STATIC)
            }.toTypedArray()
        } else {
            arrayOf(clazz)
        }
    }

    fun isEntryPointReference(reference: PsiReference) = reference is Reference

    private class Reference(
        element: JsonStringLiteral,
        range: TextRange,
        private val innerClassDepth: Int,
        private val isMethodReference: Boolean
    ) :
        PsiReferenceBase<JsonStringLiteral>(element, range),
        PsiPolyVariantReference,
        InspectionReference {

        override val description = "entry point '%s'"
        override val unresolved = resolve() == null

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return resolveReference(element, innerClassDepth, isMethodReference)
                .map { PsiElementResolveResult(it) }.toTypedArray()
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
