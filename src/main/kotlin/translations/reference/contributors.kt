/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.reference

import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.identification.TranslationIdentifier
import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.json.JsonElementTypes
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

class JavaReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        for (identifier in TranslationIdentifier.INSTANCES) {
            registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(identifier.elementClass()),
                object : PsiReferenceProvider() {
                    override fun getReferencesByElement(
                        element: PsiElement,
                        context: ProcessingContext
                    ): Array<PsiReference> {
                        val result = identifier.identifyUnsafe(element)
                        if (result != null) {
                            val referenceElement = result.referenceElement ?: return emptyArray()
                            return arrayOf(
                                TranslationReference(
                                    referenceElement,
                                    TextRange(1, referenceElement.textLength - 1),
                                    result.key
                                )
                            )
                        }
                        return emptyArray()
                    }
                }
            )
        }
    }
}

class JsonReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withElementType(JsonElementTypes.PROPERTY),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    if (!TranslationFiles.isTranslationFile(element.containingFile?.virtualFile)) {
                        return arrayOf()
                    }
                    val entry = element as? JsonProperty ?: return arrayOf()
                    val nameTextRange = element.nameElement.textRangeInParent
                    if (nameTextRange.length < 2) {
                        return arrayOf()
                    }
                    return arrayOf(
                        TranslationReference(
                            element,
                            nameTextRange.shiftRight(1).grown(-2),
                            TranslationInstance.Key("", entry.name, "")
                        ) { elem, _, newName ->
                            (elem as JsonProperty).setName(newName)
                        }
                    )
                }
            }
        )
    }
}

class LangReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withChild(PlatformPatterns.psiElement().withElementType(LangTypes.KEY)),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val entry = element as LangEntry
                    return arrayOf(
                        TranslationReference(
                            element,
                            TextRange(0, entry.key.length),
                            TranslationInstance.Key("", entry.key, "")
                        )
                    )
                }
            }
        )
    }
}
