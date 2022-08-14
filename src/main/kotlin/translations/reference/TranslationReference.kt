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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.index.TranslationInverseIndex
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException

class TranslationReference(
    element: PsiElement,
    textRange: TextRange,
    val key: TranslationInstance.Key,
    private val renameHandler: (element: PsiElement, range: TextRange, newName: String) -> PsiElement =
        { elem, range, newName ->
            ElementManipulators.getManipulator(elem).handleContentChange(elem, range, newName)!!
        }
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false), PsiPolyVariantReference {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project
        val entries = TranslationInverseIndex.findElements(
            key.full,
            GlobalSearchScope.allScope(project),
            TranslationConstants.DEFAULT_LOCALE
        )
        return entries.mapToArray(::PsiElementResolveResult)
    }

    override fun getVariants(): Array<Any?> {
        val project = myElement.project
        val defaultTranslations = TranslationIndex.getAllDefaultTranslations(project)
        val pattern = Regex("${Regex.escape(key.prefix)}(.*?)${Regex.escape(key.suffix)}")
        return defaultTranslations
            .filter { it.key.isNotEmpty() }
            .mapNotNull { entry -> pattern.matchEntire(entry.key)?.let { entry to it } }
            .map { (entry, match) ->
                LookupElementBuilder
                    .create(if (match.groups.size <= 1) entry.key else match.groupValues[1])
                    .withIcon(PlatformAssets.MINECRAFT_ICON)
                    .withTypeText(TranslationConstants.DEFAULT_LOCALE)
                    .withPresentableText(entry.key)
            }
            .toTypedArray()
    }

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        return renameHandler(myElement, rangeInElement, newElementName)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (TranslationFiles.getLocale(element.containingFile?.virtualFile) != TranslationConstants.DEFAULT_LOCALE) {
            return false
        }

        return (element is LangEntry && element.key == key.full) ||
            (element is JsonProperty && element.name == key.full)
    }
}
