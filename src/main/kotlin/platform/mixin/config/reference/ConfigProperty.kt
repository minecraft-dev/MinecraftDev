/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.MIXIN_CONFIG
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.MIXIN_SERIALIZED_NAME
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.SERIALIZED_NAME
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.MIXIN_EXTRAS_CONFIG
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.MIXIN_EXTRAS_CONFIG_KEY
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.MIXIN_EXTRAS_SERIALIZED_NAME
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.reference.InspectionReference
import com.demonwav.mcdev.util.toTypedArray
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
        val name = element.value
        customSubConfigs[name]?.let {
            return JavaPsiFacade.getInstance(element.project).findClass(it, element.resolveScope)
        }
        val configClass = findConfigClass(element) ?: return null
        return findProperty(configClass, element.value)
    }

    private fun collectVariants(context: PsiElement): Array<Any> {
        val configClass = findConfigClass(context) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val list = mutableListOf<String>()
        forEachProperty(configClass) { _, name ->
            list.add(name)
        }
        if (configClass.qualifiedName == MIXIN_CONFIG) {
            list.addAll(customSubConfigs.keys)
        }
        return list.asSequence().map { LookupElementBuilder.create(it) }.toTypedArray()
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
            val annotation = field.annotations.find { it.qualifiedName in serializedNameAnnotations }
            val name = annotation?.findDeclaredAttributeValue(null)?.constantStringValue ?: continue
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

        customSubConfigs[path.first()]?.let { newRoot ->
            path.removeFirst()
            currentClass =
                JavaPsiFacade.getInstance(context.project).findClass(newRoot, context.resolveScope)
                    ?: return null
        }

        for (i in path.lastIndex downTo 0) {
            currentClass = (findProperty(currentClass, path[i])?.type as? PsiClassType)?.resolve() ?: return null
        }
        return currentClass
    }

    private class Reference(element: JsonStringLiteral) :
        PsiReferenceBase<JsonStringLiteral>(element),
        InspectionReference {

        override fun unresolvedMessage(name: String) = MCDevBundle("reference.unresolved.config_property", name)

        override val unresolved: Boolean
            get() = resolve() == null

        override fun resolve() = resolveReference(element)
        override fun getVariants() = collectVariants(element)
        override fun isReferenceTo(element: PsiElement) = element is PsiField && super.isReferenceTo(element)
    }

    private val customSubConfigs = mapOf(
        MIXIN_EXTRAS_CONFIG_KEY to MIXIN_EXTRAS_CONFIG
    )

    private val serializedNameAnnotations = setOf(
        SERIALIZED_NAME,
        MIXIN_SERIALIZED_NAME,
        MIXIN_EXTRAS_SERIALIZED_NAME
    )
}
