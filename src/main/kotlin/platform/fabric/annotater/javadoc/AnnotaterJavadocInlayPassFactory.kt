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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterSettings
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import javax.swing.Icon

/**
 * The IDE's rendered doc comments (reader mode) would show the existing Javadocs again, so they're turned off in these
 * editors. Javadocs can't be added to the decompiled text itself, as the IDE requires decompiled classes' doc comments
 * to match their compiled stubs.
 */
class AnnotaterJavadocInlayPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (file !is PsiJavaFile || AnnotaterJavadocVirtualFile.of(file) != null) {
            return null
        }
        if (PsiDocumentManager.getInstance(file.project).getDocument(file) !== editor.document) {
            return null
        }
        // Passes are still created when nothing is displayed, so that previous inlays are removed
        return Pass(file, editor)
    }

    private class Item(
        val target: AnnotaterJavadocTarget,
        val offset: Int,
        val javadoc: String,
        val pointer: SmartPsiElementPointer<PsiMember>,
    )

    private class GutterIcon(val offset: Int, val pointer: SmartPsiElementPointer<PsiMember>)

    private class Pass(private val file: PsiJavaFile, private val editor: Editor) :
        TextEditorHighlightingPass(file.project, editor.document, false) {
        private val documentStamp = editor.document.modificationStamp

        private var showJavadocs = false
        private var items = emptyList<Item>()
        private var decorations = emptyMap<AnnotaterJavadocTarget, AnnotaterJavadocRendering.JavadocDecorations>()
        private var gutterIcons = emptyList<GutterIcon>()
        private var folds = emptyList<TextRange>()

        override fun doCollectInformation(progress: ProgressIndicator) {
            val first = file.classes.firstOrNull() ?: return
            val checkout = AnnotaterCheckoutService.instance.checkoutFor(first) ?: return
            val settings = AnnotaterSettings.instance
            showJavadocs = settings.showAnnotaterJavadocs
            val compiled = file is PsiCompiledElement
            // The Javadoc being edited is displayed by its editor instead
            val edited = AnnotaterJavadocInlineEditor.current(myProject)?.takeIf { it.hostEditor === editor }?.target

            val newItems = mutableListOf<Item>()
            // A member of each top level class with displayed Javadocs, to decorate a copy of that class with
            val outerClassMembers = mutableMapOf<String, PsiMember>()
            val newGutterIcons = mutableListOf<GutterIcon>()
            val newFolds = mutableListOf<TextRange>()
            for (member in AnnotaterJavadocTarget.declarations(file)) {
                progress.checkCanceled()
                val target = AnnotaterJavadocTarget.fromPsi(member) ?: continue
                val existing = if (compiled) null else (member as PsiJavaDocumentedElement).docComment
                if (existing != null && showJavadocs) {
                    newFolds += existing.textRange
                }
                val javadoc = checkout.javadocStore.getOrNull(target) ?: continue
                val pointer = SmartPointerManager.createPointer(member)
                newGutterIcons += GutterIcon(member.textOffset, pointer)
                if (!showJavadocs || target == edited) {
                    continue
                }
                newItems += Item(target, AnnotaterJavadocRendering.displayOffset(member), javadoc, pointer)
                outerClassMembers.putIfAbsent(target.outerClass, member)
            }
            items = newItems
            decorations = outerClassMembers.values.flatMap { member ->
                progress.checkCanceled()
                AnnotaterJavadocRendering.decorateJavadocs(
                    member,
                    checkout.javadocStore::getOrNull,
                    editor.colorsScheme,
                    settings.shortenJavadocClassNames,
                ).entries
            }.associate { it.toPair() }
            gutterIcons = newGutterIcons
            folds = newFolds
        }

        override fun doApplyInformationToEditor() {
            val document = editor.document
            if (document.modificationStamp != documentStamp) {
                return
            }
            val length = document.textLength

            editor.getUserData(Const.INLAYS_KEY)?.forEach { it.dispose() }
            val inlays = items.filter { it.offset in 0..length }.mapNotNull { item ->
                val renderer = AnnotaterJavadocInlayRenderer(
                    editor,
                    item.offset,
                    item.javadoc,
                    decorations[item.target] ?: AnnotaterJavadocRendering.JavadocDecorations.EMPTY,
                    item.pointer,
                )
                editor.inlayModel.addBlockElement(item.offset, false, true, 0, renderer)
            }
            editor.putUserData(Const.INLAYS_KEY, inlays.takeIf { it.isNotEmpty() })

            editor.getUserData(Const.GUTTER_ICONS_KEY)?.forEach { it.dispose() }
            val highlighters = gutterIcons.filter { it.offset in 0..length }.map { icon ->
                editor.markupModel.addRangeHighlighter(
                    icon.offset,
                    icon.offset,
                    HighlighterLayer.ADDITIONAL_SYNTAX,
                    null,
                    HighlighterTargetArea.LINES_IN_RANGE,
                ).also { it.gutterIconRenderer = EditGutterIconRenderer(icon.pointer) }
            }
            editor.putUserData(Const.GUTTER_ICONS_KEY, highlighters.takeIf { it.isNotEmpty() })

            val oldFolds = editor.getUserData(Const.FOLDS_KEY).orEmpty()
            if (oldFolds.isNotEmpty() || folds.isNotEmpty()) {
                val foldingModel = editor.foldingModel
                val newFolds = mutableListOf<FoldRegion>()
                foldingModel.runBatchFoldingOperation {
                    oldFolds.filter { it.isValid }.forEach(foldingModel::removeFoldRegion)
                    for (range in folds) {
                        if (range.endOffset > length) {
                            continue
                        }
                        val region = foldingModel.getFoldRegion(range.startOffset, range.endOffset)
                            ?: foldingModel.addFoldRegion(range.startOffset, range.endOffset, "…")
                                ?.also(newFolds::add)
                        region?.isExpanded = false
                    }
                }
                editor.putUserData(Const.FOLDS_KEY, newFolds.takeIf { it.isNotEmpty() })
            }

            // Internal API usage, IJPL-5327
            if (showJavadocs && DocRenderManager.isDocRenderingEnabled(editor)) {
                DocRenderManager.setDocRenderingEnabled(editor, false)
                editor.putUserData(Const.DOC_RENDERING_DISABLED_KEY, true)
            } else if (!showJavadocs && editor.getUserData(Const.DOC_RENDERING_DISABLED_KEY) == true) {
                DocRenderManager.resetEditorToDefaultState(editor)
                editor.putUserData(Const.DOC_RENDERING_DISABLED_KEY, null)
            }
        }
    }

    private class EditGutterIconRenderer(private val pointer: SmartPsiElementPointer<PsiMember>) :
        GutterIconRenderer() {
        override fun getIcon(): Icon = GeneralAssets.TINY_POTATO

        override fun getTooltipText() = MCDevBundle("fabric.annotater.javadoc.edit")

        override fun isNavigateAction() = true

        override fun getClickAction(): AnAction = EditAction(pointer)

        override fun equals(other: Any?) = other is EditGutterIconRenderer && other.pointer == pointer

        override fun hashCode() = pointer.hashCode()
    }

    class EditAction(private val pointer: SmartPsiElementPointer<PsiMember>) :
        DumbAwareAction(MCDevBundle("fabric.annotater.javadoc.edit")) {
        override fun actionPerformed(e: AnActionEvent) {
            val member = pointer.element ?: return
            AnnotaterJavadocEditing.open(member.project, member)
        }
    }

    private object Const {
        val INLAYS_KEY = Key.create<List<Inlay<*>>>("mcdev.annotater.javadocInlays")
        val FOLDS_KEY = Key.create<List<FoldRegion>>("mcdev.annotater.javadocFolds")
        val GUTTER_ICONS_KEY = Key.create<List<RangeHighlighter>>("mcdev.annotater.javadocGutterIcons")
        val DOC_RENDERING_DISABLED_KEY = Key.create<Boolean>("mcdev.annotater.docRenderingDisabled")
    }
}
