/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.translations.identification.TranslationIdentifier
import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.options.BeanConfigurable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.util.PsiTreeUtil

class TranslationCodeFoldingOptionsProvider :
    BeanConfigurable<TranslationFoldingSettings>(TranslationFoldingSettings.instance), CodeFoldingOptionsProvider {
    init {
        title = "Minecraft"
        checkBox(
            "Translation Strings",
            TranslationFoldingSettings.instance::shouldFoldTranslations
        ) {
            TranslationFoldingSettings.instance.shouldFoldTranslations = it
        }
    }
}

@State(name = "TranslationFoldingSettings", storages = [(Storage("minecraft_dev.xml"))])
class TranslationFoldingSettings : PersistentStateComponent<TranslationFoldingSettings.State> {

    data class State(
        var shouldFoldTranslations: Boolean = true
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    // State mappings
    var shouldFoldTranslations: Boolean
        get() = state.shouldFoldTranslations
        set(value) {
            state.shouldFoldTranslations = value
        }

    companion object {
        val instance: TranslationFoldingSettings
            get() = ApplicationManager.getApplication().getService(TranslationFoldingSettings::class.java)
    }
}

class TranslationFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (ApplicationManager.getApplication().isDispatchThread) {
            return emptyArray()
        }

        val descriptors = mutableListOf<FoldingDescriptor>()
        for (identifier in TranslationIdentifier.INSTANCES) {
            val elements = PsiTreeUtil.findChildrenOfType(root, identifier.elementClass())
            for (element in elements) {
                val translation = identifier.identifyUnsafe(element)
                val foldingElement = translation?.foldingElement ?: continue
                val range =
                    if (foldingElement is PsiExpressionList) {
                        val args = foldingElement.expressions.drop(translation.foldStart)
                        args.first().textRange.union(args.last().textRange)
                    } else {
                        foldingElement.textRange
                    }
                descriptors.add(
                    FoldingDescriptor(
                        translation.foldingElement.node,
                        range,
                        FoldingGroup.newGroup("mc.translation." + translation.key),
                        if (translation.formattingError == TranslationInstance.Companion.FormattingError.MISSING) {
                            "\"Insufficient parameters for formatting '${translation.text}'\""
                        } else {
                            "\"${translation.text}\""
                        }
                    )
                )
            }
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode) = "..."

    override fun isCollapsedByDefault(node: ASTNode) = TranslationFoldingSettings.instance.shouldFoldTranslations
}
