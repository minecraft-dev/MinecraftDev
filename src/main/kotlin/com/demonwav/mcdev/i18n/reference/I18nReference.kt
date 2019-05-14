/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.index.TranslationIndex
import com.demonwav.mcdev.i18n.index.TranslationInverseIndex
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
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
import com.intellij.util.indexing.FileBasedIndex

class I18nReference(
    element: PsiElement,
    textRange: TextRange,
    private val useDefault: Boolean,
    val key: String,
    private val varKey: String,
    private val renameHandler: (element: PsiElement, range: TextRange, newName: String) -> PsiElement = {
        elem, range, newName -> ElementManipulators.getManipulator(elem).handleContentChange(elem, range, newName)!!
    }
) : PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project
        val entries = TranslationInverseIndex.findElements(key, GlobalSearchScope.allScope(project), if (useDefault) I18nConstants.DEFAULT_LOCALE else null)
        return entries.mapToArray(::PsiElementResolveResult)
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return resolveResults.singleOrNull()?.element
    }

    override fun getVariants(): Array<Any?> {
        val project = myElement.project
        val entries = FileBasedIndex.getInstance().getValues(
            TranslationIndex.NAME,
            I18nConstants.DEFAULT_LOCALE,
            GlobalSearchScope.allScope(project)
        )
        val stringPattern =
            if (varKey.contains(VARIABLE_MARKER)) {
                varKey.split(VARIABLE_MARKER).joinToString("(.*?)") { Regex.escape(it) }
            } else {
                "(" + Regex.escape(varKey) + ".*?)"
            }
        val pattern = Regex(stringPattern)
        return entries
            .asSequence()
            .flatMap { it.translations.asSequence() }
            .filter { it.key.isNotEmpty() }
            .mapNotNull { entry -> pattern.matchEntire(entry.key)?.let { entry to it } }
            .map { (entry, match) ->
                LookupElementBuilder
                    .create(if (match.groups.size <= 1) entry.key else match.groupValues[1])
                    .withIcon(PlatformAssets.MINECRAFT_ICON)
                    .withTypeText(I18nConstants.DEFAULT_LOCALE)
                    .withPresentableText(entry.key)
            }
            .toTypedArray()
    }

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        val stringPattern =
            if (varKey.contains(VARIABLE_MARKER)) {
                varKey.split(VARIABLE_MARKER).joinToString("(.*?)") { Regex.escape(it) }
            } else {
                "(" + Regex.escape(varKey) + ")"
            }
        val pattern = Regex(stringPattern)
        val match = pattern.matchEntire(newElementName)
        val newName = if (match != null && match.groups.size > 1) match.groupValues[1] else newElementName
        return renameHandler(myElement, rangeInElement, newName)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return (element is I18nEntry && element.key == key) || (element is JsonProperty && element.name == key)
    }

    companion object {
        const val VARIABLE_MARKER = "\$IDEA_TRANSLATION_VARIABLE"
    }
}
