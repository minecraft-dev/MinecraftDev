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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckout
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterListener
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterNotifications
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.ColorUtil
import com.intellij.ui.EditorNotifications
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.IOException
import java.util.concurrent.CompletableFuture
import javax.swing.JPanel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The editing of one annotater Javadoc, in an editor embedded above its declaration in the Minecraft class it's
 * displayed in. Only one Javadoc is edited at a time in each project; see [current].
 *
 * The embedded editor edits [file], a copy of the class's source file, so that the IDE's Java support works as it
 * would in a real source file. Everything except the edited doc comment is folded away and guarded against edits.
 * The copy isn't updated if the annotater Javadocs change on disk while it's being edited, and saving only writes the
 * edited Javadoc.
 */
class AnnotaterJavadocInlineEditor private constructor(
    val project: Project,
    val file: AnnotaterJavadocVirtualFile,
    private val document: Document,
    /**
     * Kept so that editing can be restarted when the checkout switches branch.
     */
    val source: SmartPsiElementPointer<PsiMember>?,
) : Disposable {
    /**
     * Delayed save until the user stops typing.
     */
    private var saveJob: Job? = null

    private val guards = mutableListOf<RangeMarker>()

    @Volatile
    private var dirty = false

    @Volatile
    var hostEditor: Editor? = null
        private set

    var editor: EditorEx? = null
        private set

    val checkout: AnnotaterCheckout
        get() = file.checkout

    val target: AnnotaterJavadocTarget
        get() = file.target

    init {
        installGuardedBlocks()
        document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    dirty = true
                    saveJob?.cancel()
                    saveJob = project.service<ProjectScope>().coroutineScope.launch(Dispatchers.EDT) {
                        delay(SAVE_DELAY_MS.milliseconds)
                        // Otherwise save would cancel this job, which cancels the save partway through
                        saveJob = null
                        save()
                    }
                }
            },
            this,
        )
    }

    /**
     * The whole lines of the edited doc comment, excluding the line break at the end.
     */
    private fun commentRange(): TextRange? {
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitDocument(document)
        val psiFile = documentManager.getPsiFile(document) as? PsiJavaFile ?: return null
        val member = file.copy.memberOf(psiFile, target) ?: return null
        val comment = (member as PsiJavaDocumentedElement).docComment ?: return null
        val start = document.getLineStartOffset(document.getLineNumber(comment.textRange.startOffset))
        val end = document.getLineEndOffset(document.getLineNumber(comment.textRange.endOffset))
        return TextRange(start, end)
    }

    /**
     * Makes everything outside of the edited doc comment read-only. The line break after the comment is read-only too,
     * so that the declaration can't be joined onto the comment. The guards are greedy at the ends of the document, so
     * nothing can be inserted there, but not next to the comment, so that text can be inserted at its start and end.
     */
    private fun installGuardedBlocks() {
        val range = commentRange() ?: return
        addGuard(0, range.startOffset, greedyToLeft = true, greedyToRight = false)
        addGuard(range.endOffset, document.textLength, greedyToLeft = false, greedyToRight = true)
    }

    private fun addGuard(start: Int, end: Int, greedyToLeft: Boolean, greedyToRight: Boolean) {
        if (end <= start) {
            return
        }
        val guard = document.createGuardedBlock(start, end)
        guard.isGreedyToLeft = greedyToLeft
        guard.isGreedyToRight = greedyToRight
        guards += guard
    }

    fun show(host: EditorEx, offset: Int) {
        val range = commentRange() ?: return
        val editor = EditorFactory.getInstance().createEditor(document, project, file, false, EditorKind.MAIN_EDITOR)
            as EditorEx
        this.editor = editor
        hostEditor = host
        configure(editor, host)

        // Hide everything except the doc comment. Each fold includes a line break, so it doesn't leave an empty line
        editor.foldingModel.runBatchFoldingOperation {
            editor.foldingModel.createFoldRegion(0, range.startOffset, "", null, true)
            editor.foldingModel.createFoldRegion(range.endOffset, document.textLength, "", null, true)
        }
        // The end of the last line is displayed at the end of the hidden text after it, and the start of the first line
        // at the start of the hidden text before it, so keep the caret within the comment
        editor.caretModel.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) {
                    val caret = event.caret
                    val start = guards.firstOrNull()?.endOffset ?: return
                    val end = guards.lastOrNull()?.startOffset ?: return
                    if (caret.offset < start) {
                        caret.moveToOffset(start)
                    } else if (caret.offset > end) {
                        caret.moveToOffset(end)
                    }
                }
            },
            this,
        )
        FinishAction().registerCustomShortcutSet(CommonShortcuts.ESCAPE, editor.contentComponent, this)

        val component = object : JPanel(BorderLayout()), UiDataProvider {
            override fun uiDataSnapshot(sink: DataSink) {
                // Otherwise actions such as undo use the host editor, which is the file editor this is embedded in
                sink[PlatformCoreDataKeys.FILE_EDITOR] = TextEditorProvider.getInstance().getTextEditor(editor)
                sink[CommonDataKeys.VIRTUAL_FILE] = file
            }

            override fun getPreferredSize(): Dimension {
                val insets = insets
                return Dimension(
                    super.getPreferredSize().width,
                    editor.contentComponent.preferredSize.height + insets.top + insets.bottom,
                )
            }
        }
        component.border = JBUI.Borders.customLine(
            ColorUtil.mix(host.colorsScheme.defaultBackground, host.colorsScheme.defaultForeground, 0.25),
            1,
        )
        component.add(editor.component, BorderLayout.CENTER)
        // The embedded editor's height depends on its text and its width, which changes with the host editor's width
        document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    ApplicationManager.getApplication().invokeLater({ component.revalidate() }) { isDisposed }
                }
            },
            this,
        )
        component.addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    ApplicationManager.getApplication().invokeLater({ component.revalidate() }) { isDisposed }
                }
            },
        )

        val inlay = EditorEmbeddedComponentManager.getInstance().addComponent(
            host,
            component,
            EditorEmbeddedComponentManager.Properties(
                EditorEmbeddedComponentManager.ResizePolicy.none(),
                null,
                false,
                true,
                false,
                true,
                0,
                offset,
            ),
        )
        inlay?.let { Disposer.register(this, it) }

        // Finish editing when the Minecraft class is closed
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorReleased(event: EditorFactoryEvent) {
                    if (event.editor === host) {
                        finish(force = true)
                    }
                }
            },
            this,
        )

        host.scrollingModel.scrollTo(host.offsetToLogicalPosition(offset), ScrollType.MAKE_VISIBLE)
        editor.caretModel.moveToOffset(document.getLineEndOffset(document.getLineNumber(range.startOffset)))
        IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
        restartHostHighlighting()
    }

    /**
     * Finishes editing when Escape is pressed, unless there's something else for Escape to close.
     */
    private inner class FinishAction : DumbAwareAction() {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            val editor = editor
            e.presentation.isEnabled = editor != null &&
                LookupManager.getActiveLookup(editor) == null &&
                !editor.selectionModel.hasSelection() &&
                editor.caretModel.caretCount == 1
        }

        override fun actionPerformed(e: AnActionEvent) {
            HintManager.getInstance().hideAllHints()
            finish()
        }
    }

    private fun configure(editor: EditorEx, host: EditorEx) {
        editor.settings.apply {
            isLineNumbersShown = false
            isFoldingOutlineShown = false
            isLineMarkerAreaShown = false
            setGutterIconsShown(false)
            isRightMarginShown = false
            isIndentGuidesShown = false
            isVirtualSpace = false
            isAdditionalPageAtBottom = false
            additionalLinesCount = 0
            additionalColumnsCount = 0
            isUseSoftWraps = true
            // Java folds doc comments and imports by default, which would hide the edited comment
            isAutoCodeFoldingEnabled = false
        }
        editor.setVerticalScrollbarVisible(false)
        editor.setHorizontalScrollbarVisible(false)
        // Let the host editor scroll instead
        editor.scrollPane.isWheelScrollingEnabled = false
        editor.colorsScheme.setEditorFontSize(host.colorsScheme.editorFontSize2D)
        editor.setBackgroundColor(
            ColorUtil.mix(host.colorsScheme.defaultBackground, host.colorsScheme.defaultForeground, 0.04),
        )
        // Internal API usage, IJPL-5327
        DocRenderManager.setDocRenderingEnabled(editor, false)
    }

    private fun restartHostHighlighting() {
        val host = hostEditor ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(host.document) ?: return
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile, this)
    }

    private val isDisposed: Boolean
        get() = project.getUserData(CURRENT_KEY) !== this

    /**
     * Saves pending changes to the annotater checkout. The returned future completes with false if the Javadoc
     * couldn't be saved, in which case [AnnotaterJavadocVirtualFile.error] says why.
     */
    fun save(): CompletableFuture<Boolean> {
        ApplicationManager.getApplication().assertIsDispatchThread()
        saveJob?.cancel()
        saveJob = null
        if (!dirty || project.isDisposed) {
            return CompletableFuture.completedFuture(file.error == null)
        }
        dirty = false

        return when (val result = extract()) {
            is AnnotaterJavadocSourceCopy.ExtractResult.Error -> {
                saved(result.message)
                CompletableFuture.completedFuture(false)
            }

            is AnnotaterJavadocSourceCopy.ExtractResult.Success -> {
                val javadoc = result.javadocs[target]
                // Writing the mapping file is slow, so do it in the background, one save at a time
                CompletableFuture.supplyAsync(
                    {
                        val error = write(javadoc)
                        ApplicationManager.getApplication().invokeLater { saved(error) }
                        error == null
                    },
                    saveExecutor,
                )
            }
        }
    }

    private fun extract(): AnnotaterJavadocSourceCopy.ExtractResult {
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitDocument(document)
        val psiFile = documentManager.getPsiFile(document) as? PsiJavaFile
            ?: return AnnotaterJavadocSourceCopy.ExtractResult.Error(
                MCDevBundle("fabric.annotater.javadoc.error.structure_changed"),
            )
        return file.copy.extractJavadocs(psiFile) { isGuarded(document, it) }
    }

    private fun saved(error: String?) {
        if (error != null) {
            dirty = true
        }
        if (error != file.error) {
            file.error = error
            EditorNotifications.getInstance(project).updateAllNotifications()
        }
    }

    private fun write(javadoc: String?): String? {
        val store = checkout.javadocStore
        val outerClass = file.outerClass
        return try {
            store.invalidate(outerClass)
            if (store.get(target) != javadoc) {
                store.setAll(mapOf(target to javadoc))
                // Refreshing only the file wouldn't notice it if the VFS hasn't loaded it yet, such as when it's created
                val mappingFile = store.fileFor(outerClass).toFile()
                VfsUtil.markDirtyAndRefresh(false, false, true, mappingFile.parentFile, mappingFile)
                ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC)
                    .javadocsChanged(checkout.root, outerClass)
            }
            null
        } catch (e: IOException) {
            thisLogger().info("Failed to save annotater Javadoc of $target", e)
            MCDevBundle("fabric.annotater.javadoc.error.io", e.message ?: e.toString())
        }
    }

    /**
     * Saves the Javadoc and stops editing it. If it can't be saved because the user wrote something annotater doesn't
     * support, editing continues so that the user can fix it, unless [force] is true.
     *
     * @return whether editing stopped
     */
    fun finish(force: Boolean = false): Boolean {
        if (isDisposed) {
            return true
        }
        if (!force && dirty && extract() is AnnotaterJavadocSourceCopy.ExtractResult.Error) {
            save()
            editor?.let { editor ->
                file.error?.let { HintManager.getInstance().showErrorHint(editor, it) }
            }
            return false
        }
        save().thenAccept { success ->
            if (!success) {
                ApplicationManager.getApplication().invokeLater {
                    AnnotaterNotifications.notifyInfo(
                        project,
                        MCDevBundle("fabric.annotater.javadoc.save_failed"),
                        file.error.orEmpty(),
                    )
                }
            }
        }
        Disposer.dispose(this)
        return true
    }

    override fun dispose() {
        val wasCurrent = project.getUserData(CURRENT_KEY) === this
        if (wasCurrent) {
            project.putUserData(CURRENT_KEY, null)
        }
        saveJob?.cancel()
        saveJob = null
        editor?.let(EditorFactory.getInstance()::releaseEditor)
        editor = null
        for (guard in guards) {
            document.removeGuardedBlock(guard)
        }
        guards.clear()
        if (wasCurrent && !project.isDisposed) {
            restartHostHighlighting()
            EditorNotifications.getInstance(project).updateAllNotifications()
        }
        hostEditor = null
    }

    companion object {
        private const val SAVE_DELAY_MS = 750L

        private val CURRENT_KEY = Key.create<AnnotaterJavadocInlineEditor>("mcdev.annotater.javadocInlineEditor")

        private val saveExecutor =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Annotater Javadoc saving", 1)

        fun current(project: Project): AnnotaterJavadocInlineEditor? = project.getUserData(CURRENT_KEY)

        /**
         * The parent of the inline editors of a project, so they're disposed with it, and the scope of their
         * coroutines. Plugins shouldn't use the project itself as a parent disposable, as that leaks the plugin's
         * classes if it's unloaded while the project is open.
         */
        @Service(Service.Level.PROJECT)
        private class ProjectScope(val coroutineScope: CoroutineScope) : Disposable {
            override fun dispose() {
            }
        }

        /**
         * Whether [range] of [document] is entirely within one guarded block, i.e. the user didn't write it.
         */
        fun isGuarded(document: Document, range: TextRange): Boolean {
            val guard = document.getRangeGuard(range.startOffset, range.endOffset) ?: return false
            return guard.startOffset <= range.startOffset && range.endOffset <= guard.endOffset
        }

        /**
         * Finishes the previous editing in the project first. Returns null if that couldn't be finished, or the
         * target's doc comment can't be found in [file].
         */
        fun start(
            project: Project,
            file: AnnotaterJavadocVirtualFile,
            source: SmartPsiElementPointer<PsiMember>? = null,
        ): AnnotaterJavadocInlineEditor? {
            current(project)?.let { previous ->
                if (!previous.finish()) {
                    return null
                }
            }
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
            val inlineEditor = AnnotaterJavadocInlineEditor(project, file, document, source)
            if (inlineEditor.guards.isEmpty()) {
                Disposer.dispose(inlineEditor)
                return null
            }
            project.putUserData(CURRENT_KEY, inlineEditor)
            Disposer.register(project.service<ProjectScope>(), inlineEditor)
            EditorNotifications.getInstance(project).updateAllNotifications()
            return inlineEditor
        }
    }
}
