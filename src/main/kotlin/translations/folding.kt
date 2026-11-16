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

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.asset.MCDevBundle
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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.textRange
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

class TranslationCodeFoldingOptionsProvider :
    BeanConfigurable<TranslationFoldingSettings>(TranslationFoldingSettings.instance), CodeFoldingOptionsProvider {
    init {
        title = MCDevBundle("translations.folding.title")
        checkBox(
            MCDevBundle("translations.folding.translation_strings"),
            TranslationFoldingSettings.instance::shouldFoldTranslations,
        ) {
            TranslationFoldingSettings.instance.shouldFoldTranslations = it
        }
    }
}

@State(name = "TranslationFoldingSettings", storages = [(Storage("minecraft_dev.xml"))])
class TranslationFoldingSettings : PersistentStateComponent<TranslationFoldingSettings.State> {

    data class State(
        var shouldFoldTranslations: Boolean = true,
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
            return FoldingDescriptor.EMPTY_ARRAY
        }

        val descriptors = mutableListOf<FoldingDescriptor>()
        val uElement = root.toUElement() ?: return FoldingDescriptor.EMPTY_ARRAY
        val translations = mutableListOf<TranslationInstance>()
        uElement.accept(object : AbstractUastVisitor() {
            override fun visitExpression(node: UExpression): Boolean {
                val translation = TranslationIdentifier.identify(node)
                if (translation != null) {
                    translations += translation
                }

                return super.visitElement(node)
            }
        })
        for (translation in translations) {
            if (!translation.shouldFold) {
                continue
            }
            val foldingElement = translation.foldingElement ?: continue
            val range =
                if (foldingElement is UCallExpression && translation.foldStart != 0) {
                    val args = foldingElement.valueArguments.drop(translation.foldStart)
                    val startRange = args.first().textRange ?: continue
                    val endRange = args.last().textRange ?: continue
                    startRange.union(endRange)
                } else {
                    foldingElement.textRange ?: continue
                }
            if (!translation.required && translation.formattingError != null) {
                continue
            }
            descriptors.add(
                FoldingDescriptor(
                    translation.foldingElement.sourcePsi?.node!!,
                    range,
                    FoldingGroup.newGroup("mc.translation." + translation.key),
                    if (translation.formattingError == TranslationInstance.FormattingError.MISSING) {
                        "\"${MCDevBundle("translations.folding.insufficient_parameters", translation.text)}\""
                    } else {
                        "\"${translation.text}\""
                    },
                ),
            )
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode) = "..."

    override fun isCollapsedByDefault(node: ASTNode) = TranslationFoldingSettings.instance.shouldFoldTranslations
}
