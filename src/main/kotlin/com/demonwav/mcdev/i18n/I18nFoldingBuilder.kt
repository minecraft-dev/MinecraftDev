/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.translations.Translation
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

class I18nFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean) = Translation.fold(root)

    override fun getPlaceholderText(node: ASTNode) = "..."

    override fun isCollapsedByDefault(node: ASTNode) = I18nFoldingSettings.instance.shouldFoldTranslations
}
