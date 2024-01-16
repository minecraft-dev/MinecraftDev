/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.manipulator
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.ArrayUtil
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
        val memberParts = text.split("::", limit = 2)
        val clazzParts = memberParts[0].split("$", limit = 0)
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
                    false,
                ),
            )
            cursor += clazzPart.length
        }
        if (memberParts.size == 2) {
            cursor += 2
            references.add(
                Reference(
                    element,
                    range.cutOut(TextRange.from(cursor, memberParts[1].length)),
                    innerClassDepth,
                    true,
                ),
            )
        }
        return references.toTypedArray()
    }

    private fun resolveReference(
        element: JsonStringLiteral,
        innerClassDepth: Int,
        isMemberReference: Boolean,
    ): Array<PsiElement> {
        val strReference = element.value
        val memberParts = strReference.split("::", limit = 2)
        // split at dollar sign for inner class evaluation
        val clazzParts = memberParts[0].split("$", limit = 0)
        // this case should only happen if someone misuses the method, better protect against it anyways
        if (innerClassDepth >= clazzParts.size ||
            innerClassDepth + 1 < clazzParts.size &&
            isMemberReference
        ) {
            throw IncorrectOperationException("Invalid reference")
        }
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
        return if (isMemberReference) {
            if (memberParts.size == 1) {
                throw IncorrectOperationException("Invalid reference")
            }

            val members = mutableListOf<PsiElement>()
            clazz.fields.filterTo(members) { field ->
                field.name == memberParts[1] &&
                    field.hasModifierProperty(PsiModifier.PUBLIC) &&
                    field.hasModifierProperty(PsiModifier.STATIC)
            }

            clazz.methods.filterTo(members) { method ->
                method.name == memberParts[1] &&
                    method.hasModifierProperty(PsiModifier.PUBLIC)
            }

            members.toTypedArray()
        } else {
            arrayOf(clazz)
        }
    }

    fun isEntryPointReference(reference: PsiReference) = reference is Reference

    fun isValidEntrypointClass(element: PsiClass): Boolean {
        val psiFacade = JavaPsiFacade.getInstance(element.project)
        var inheritsEntrypointInterface = false
        for (entrypoint in FabricConstants.ENTRYPOINTS) {
            val entrypointClass = psiFacade.findClass(entrypoint, element.resolveScope)
                ?: continue
            if (element.isInheritor(entrypointClass, true)) {
                inheritsEntrypointInterface = true
                break
            }
        }
        return inheritsEntrypointInterface
    }

    fun isValidEntrypointField(field: PsiField): Boolean {
        if (!field.hasModifierProperty(PsiModifier.PUBLIC) || !field.hasModifierProperty(PsiModifier.STATIC)) {
            return false
        }

        val fieldTypeClass = (field.type as? PsiClassType)?.resolve()
        return fieldTypeClass != null && isValidEntrypointClass(fieldTypeClass)
    }

    fun isValidEntrypointMethod(method: PsiMethod): Boolean {
        return method.hasModifierProperty(PsiModifier.PUBLIC) && !method.hasParameters()
    }

    class Reference(
        element: JsonStringLiteral,
        range: TextRange,
        private val innerClassDepth: Int,
        private val isMemberReference: Boolean,
    ) :
        PsiReferenceBase<JsonStringLiteral>(element, range),
        PsiPolyVariantReference,
        InspectionReference {

        override val description = "entry point '%s'"
        override val unresolved = resolve() == null

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return resolveReference(element, innerClassDepth, isMemberReference)
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

            if (isMemberReference) {
                val newTargetName = when (newTarget) {
                    is PsiMethod -> newTarget.name
                    is PsiField -> newTarget.name
                    else -> throw IncorrectOperationException("Cannot target $newTarget")
                }
                if (parts.size == 1) {
                    throw IncorrectOperationException("Invalid reference")
                }
                val memberRange = range.cutOut(TextRange.from(parts[0].length + 2, parts[1].length))
                return manipulator.handleContentChange(element, memberRange, newTargetName)
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

        override fun getVariants(): Array<Any> {
            val manipulator = element.manipulator
                ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

            val range = manipulator.getRangeInElement(element)
            val text = element.text.substring(range.startOffset, range.endOffset)
            val parts = text.split("::", limit = 2)

            val variants = mutableListOf<Any>()
            if (!isMemberReference) {
                val psiFacade = JavaPsiFacade.getInstance(element.project)
                for (entrypoint in FabricConstants.ENTRYPOINTS) {
                    val entrypointClass = psiFacade.findClass(entrypoint, element.resolveScope)
                        ?: continue
                    ClassInheritorsSearch.search(entrypointClass, true)
                        .mapNotNullTo(variants) {
                            val shortName = it.name ?: return@mapNotNullTo null
                            val fqName = it.fullQualifiedName ?: return@mapNotNullTo null
                            JavaLookupElementBuilder.forClass(it, fqName, true).withPresentableText(shortName)
                        }
                }
            } else if (parts.size >= 2) {
                val psiFacade = JavaPsiFacade.getInstance(element.project)
                val className = parts[0].replace('$', '.')
                val clazz = psiFacade.findClass(className, element.resolveScope)
                if (clazz != null) {
                    clazz.fields.filterTo(variants, ::isValidEntrypointField)
                    clazz.methods.filterTo(variants, ::isValidEntrypointMethod)
                }
            }

            return variants.toTypedArray()
        }
    }
}
