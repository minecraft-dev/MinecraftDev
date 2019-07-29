/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.findDefaultLangEntries
import com.demonwav.mcdev.i18n.findLangEntries
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.toArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.IncorrectOperationException

class I18nReference(
    element: PsiElement,
    textRange: TextRange,
    private val useDefault: Boolean,
    val key: String,
    private val varKey: String
) : PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project
        val entries = if (useDefault) project.findDefaultLangEntries(key = key) else project.findLangEntries(key = key)
        return entries.mapToArray(::PsiElementResolveResult)
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return resolveResults.singleOrNull()?.element
    }

    override fun getVariants(): Array<Any?> {
        val project = myElement.project
        val entries = project.findDefaultLangEntries()
        val stringPattern =
            if (varKey.contains(VARIABLE_MARKER)) {
                varKey.split(VARIABLE_MARKER).joinToString("(.*?)") { Regex.escape(it) }
            } else {
                "(" + Regex.escape(varKey) + ".*?)"
            }
        val pattern = Regex(stringPattern)
        return entries
            .filter { it.key.isNotEmpty() }
            .mapNotNull { entry -> pattern.matchEntire(entry.key)?.let { entry to it } }
            .map { (entry, match) ->
                LookupElementBuilder
                    .create(if (match.groups.size <= 1) entry.key else match.groupValues[1])
                    .withIcon(PlatformAssets.MINECRAFT_ICON)
                    .withTypeText(entry.containingFile.name)
                    .withPresentableText(entry.key)
            }
            .toArray()
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
        return super.handleElementRename(
            if (match != null && match.groups.size > 1) match.groupValues[1] else newElementName
        )
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return element is I18nEntry && element.key == key
    }

    companion object {
        const val VARIABLE_MARKER = "\$IDEA_TRANSLATION_VARIABLE"
    }
}
