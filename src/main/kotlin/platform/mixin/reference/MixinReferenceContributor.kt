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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.reference.target.FieldDefinitionReference
import com.demonwav.mcdev.platform.mixin.reference.target.MethodDefinitionReference
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.util.PlatformIcons

class MixinReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Method references
        registrar.registerReferenceProvider(
            MethodReference.ELEMENT_PATTERN,
            MethodReference,
        )

        // Desc references
        registrar.registerReferenceProvider(
            DescReference.ELEMENT_PATTERN,
            DescReference,
        )

        // Injection point types
        registrar.registerReferenceProvider(
            InjectionPointReference.ELEMENT_PATTERN,
            InjectionPointReference,
        )

        // Target references
        registrar.registerReferenceProvider(
            TargetReference.ELEMENT_PATTERN,
            TargetReference,
        )

        // Accessor references
        registrar.registerReferenceProvider(
            AccessorReference.ELEMENT_PATTERN,
            AccessorReference,
        )
        registrar.registerReferenceProvider(
            InvokerReference.ELEMENT_PATTERN,
            InvokerReference,
        )

        // Definition references
        registrar.registerReferenceProvider(
            FieldDefinitionReference.ELEMENT_PATTERN,
            FieldDefinitionReference,
        )
        registrar.registerReferenceProvider(
            MethodDefinitionReference.ELEMENT_PATTERN,
            MethodDefinitionReference,
        )

        registrar.registerReferenceProvider(
            MixinReferences.MIXIN_TARGETS,
            MixinTargetsClassReferenceProvider(),
        )
    }
}

class MixinTargetsClassReferenceProvider : JavaClassReferenceProvider() {
    init {
        setOption(ALLOW_DOLLAR_NAMES, true)
        setOption(JVM_FORMAT, true)
        setOption(ADVANCED_RESOLVE, true)
        setOption(RESOLVE_QUALIFIED_CLASS_NAME, true)
    }

    override fun getReferencesByString(
        str: String,
        position: PsiElement,
        offsetInPosition: Int,
    ): Array<out PsiReference?> {
        return object : JavaClassReferenceSet(str, position, offsetInPosition, true, this) {
            override fun isAllowDollarInNames(): Boolean = true

            override fun createReference(
                referenceIndex: Int,
                referenceText: String,
                textRange: TextRange,
                staticImport: Boolean,
            ): JavaClassReference {
                return MixinTargetsClassReference(this, referenceIndex, referenceText, textRange, staticImport)
            }
        }.allReferences
    }
}

class MixinTargetsClassReference(
    referenceSet: JavaClassReferenceSet,
    referenceIndex: Int,
    referenceText: String,
    textRange: TextRange,
    staticImport: Boolean,
) : JavaClassReference(referenceSet, textRange, referenceIndex, referenceText, staticImport) {

    override fun getVariants(): Array<Any> {
        val original = super.getVariants()
        val list = ArrayList<Any>()
        for (variant in original) {
            val target = if (variant is LookupElement) variant.`object` else variant
            if (target is PsiPackage) {
                val lookupText = if (variant is LookupElement) variant.lookupString else (target.name ?: "")
                val text = if (lookupText.endsWith(".")) lookupText else "$lookupText."
                list.add(
                    LookupElementBuilder.create(target, text)
                        .withPresentableText(text)
                        .withIcon(PlatformIcons.PACKAGE_ICON)
                        .withInsertHandler { context, _ ->
                            AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
                        },
                )
            } else if (target is PsiClass) {
                list.add(variant)
            }
        }
        return list.toArray()
    }
}
