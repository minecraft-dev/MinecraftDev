/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.MIXIN_CONFIG
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.SERIALIZED_NAME
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext

object ConfigProperty : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
        arrayOf(Reference(element as JsonStringLiteral))

    fun resolveReference(element: JsonStringLiteral): PsiElement? {
        val configClass = findConfigClass(element) ?: return null
        return findProperty(configClass, element.value)
    }

    private fun collectVariants(context: PsiElement): Array<Any> {
        val configClass = findConfigClass(context) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val list = ArrayList<LookupElementBuilder>()
        forEachProperty(configClass) { _, name ->
            list.add(LookupElementBuilder.create(name))
        }
        return list.toArray()
    }

    private fun findProperty(configClass: PsiClass, name: String): PsiField? {
        forEachProperty(configClass) { field, fieldName ->
            if (fieldName == name) {
                return field
            }
        }

        return null
    }

    private inline fun forEachProperty(configClass: PsiClass, func: (PsiField, String) -> Unit) {
        for (field in configClass.fields) {
            val name =
                field.findAnnotation(SERIALIZED_NAME)?.findDeclaredAttributeValue(null)?.constantStringValue ?: continue
            func(field, name)
        }
    }

    private fun findConfigClass(context: PsiElement): PsiClass? {
        val mixinConfig =
            JavaPsiFacade.getInstance(context.project).findClass(MIXIN_CONFIG, context.resolveScope) ?: return null

        val property = context.parent as JsonProperty

        val path = ArrayList<String>()

        var current = property.parent
        while (current != null && current !is PsiFile) {
            if (current is JsonProperty) {
                path.add(current.name)
            }
            current = current.parent
        }

        path.ifEmpty { return mixinConfig }

        // Walk to correct class
        var currentClass = mixinConfig
        for (i in path.lastIndex downTo 0) {
            currentClass = (findProperty(currentClass, path[i])?.type as? PsiClassType)?.resolve() ?: return null
        }
        return currentClass
    }

    private class Reference(element: JsonStringLiteral) : PsiReferenceBase<JsonStringLiteral>(element),
        InspectionReference {

        override val description: String
            get() = "config property '%s'"

        override val unresolved: Boolean
            get() = resolve() == null

        override fun resolve() = resolveReference(element)
        override fun getVariants() = collectVariants(element)
        override fun isReferenceTo(element: PsiElement) = element is PsiField && super.isReferenceTo(element)
    }
}
