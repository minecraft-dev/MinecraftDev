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

package com.demonwav.mcdev.platform.fabric.annotater

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocCodeStyleSettingsModifier
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocInlineEditor
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocIntentionActionFilter
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocRendering
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocSourceCopy
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocTarget
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocUnqualifiedReferenceInspection
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocVirtualFile
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.ide.DataManager
import com.intellij.ide.impl.HeadlessDataManager
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.modifier.TransientCodeStyleSettings
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(EdtInterceptor::class)
@DisplayName("Annotater Javadoc Editor Tests")
class AnnotaterJavadocEditorTest : BaseMinecraftTest() {
    @TempDir
    lateinit var root: Path

    private lateinit var checkout: AnnotaterCheckout

    private val clamp = AnnotaterJavadocTarget.MethodTarget("net/minecraft/util/Mth", "clamp", "(III)I")

    @BeforeEach
    fun setUpProject() {
        checkout = AnnotaterCheckout(root, AnnotaterConfig("26.3", "mappings", null), managed = false)
        buildProject {
            java(
                "net/minecraft/util/Mth.java",
                """
                package net.minecraft.util;

                /** Existing class docs. */
                public class Mth {
                    /** Existing method docs. */
                    public static int clamp(int value, int min, int max) {
                        // Not clamped yet
                        return value;
                    }

                    public static final float PI = 3.14F;
                }
                """.trimIndent(),
            )
            java(
                "net/minecraft/util/Ticker.java",
                """
                package net.minecraft.util;

                public class Ticker<T> {
                    public <U> void tick() {
                        class Step<V> {
                            int count;
                        }
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                            }
                        };
                    }
                }
                """.trimIndent(),
            )
            java(
                "net/minecraft/world/level/Level.java",
                """
                package net.minecraft.world.level;

                public class Level {
                    public boolean addEntity(net.minecraft.util.Ticker<?> ticker, String[] names, int count) {
                        return  true ;
                    }
                }
                """.trimIndent(),
            )
        }
    }

    private fun findMember(className: String, name: String? = null): PsiMember {
        val clazz = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))!!
        return when (name) {
            null -> clazz
            else -> clazz.findMethodsByName(name, false).firstOrNull() ?: clazz.findFieldByName(name, false)!!
        }
    }

    /**
     * Tests use temporary code style settings, which code style settings modifiers aren't applied to.
     */
    private fun withAnnotaterCodeStyle(action: () -> Unit) {
        val settings = TransientCodeStyleSettings(fixture.file.virtualFile, project, CodeStyle.getSettings(project))
        AnnotaterJavadocCodeStyleSettingsModifier().modifySettings(settings, fixture.file)
        CodeStyle.doWithTemporarySettings(project, settings, Runnable(action))
    }

    private val pi = AnnotaterJavadocTarget.FieldTarget("net/minecraft/util/Mth", "PI", "F")

    private fun clampCopy(javadoc: String?, placeholderFor: AnnotaterJavadocTarget? = null) =
        AnnotaterJavadocSourceCopy.create(findMember("net.minecraft.util.Mth", "clamp"), placeholderFor) {
            if (it == clamp) javadoc else null
        }!!

    @Test
    @DisplayName("Source copy replaces the existing Javadocs with Markdown annotater Javadocs")
    fun sourceCopyText() {
        val copy = clampCopy("Clamps a value.\n\n@param value the value")
        assertEquals(
            """
            package net.minecraft.util;

            public class Mth {
                /// Clamps a value.
                ///
                /// @param value the value
                public static int clamp(int value, int min, int max) {
                    // Not clamped yet
                    return value;
                }

                public static final float PI = 3.14F;
            }
            """.trimIndent(),
            copy.text,
        )
        assertEquals("Mth.java", copy.fileName)
        assertEquals(listOf(AnnotaterJavadocTarget.ClassTarget("net/minecraft/util/Mth"), clamp, pi), copy.targets)
    }

    @Test
    @DisplayName("Local classes and members of local and anonymous classes can be documented")
    fun localAndAnonymousClasses() {
        val copy = AnnotaterJavadocSourceCopy.create(findMember("net.minecraft.util.Ticker")) { null }!!
        assertEquals(
            listOf(
                AnnotaterJavadocTarget.ClassTarget("net/minecraft/util/Ticker"),
                AnnotaterJavadocTarget.MethodTarget("net/minecraft/util/Ticker", "tick", "()V"),
                AnnotaterJavadocTarget.ClassTarget("net/minecraft/util/Ticker\$1Step"),
                AnnotaterJavadocTarget.FieldTarget("net/minecraft/util/Ticker\$1Step", "count", "I"),
                AnnotaterJavadocTarget.MethodTarget("net/minecraft/util/Ticker\$1", "run", "()V"),
            ),
            // Type parameters and anonymous classes are also PsiClasses, but aren't declarations that can be documented
            copy.targets,
        )
        assertEquals("net/minecraft/util/Ticker", copy.outerClass)
    }

    @Test
    @DisplayName("Source copy adds a placeholder Javadoc to the edited member")
    fun sourceCopyPlaceholder() {
        val copy = clampCopy(null, placeholderFor = clamp)
        assertTrue(copy.text.contains("public class Mth {\n    /// \n    public static int clamp("), copy.text)
    }

    @Test
    @DisplayName("Javadocs are extracted from Markdown doc comments")
    fun extract() {
        val copy = clampCopy("Clamps a value.\n\n@param value the value")
        val file = copy.createPsiFile(project)!!
        val method = copy.memberOf(file, clamp) as PsiMethod
        assertTrue(method.docComment!!.isMarkdownComment)
        assertEquals(clamp, copy.targetOf(file, method))
        assertEquals(
            AnnotaterJavadocSourceCopy.ExtractResult.Success(
                mapOf(
                    AnnotaterJavadocTarget.ClassTarget("net/minecraft/util/Mth") to null,
                    pi to null,
                    clamp to "Clamps a value.\n\n@param value the value",
                ),
            ),
            // The comment in the method body is part of the original source
            copy.extractJavadocs(file) { it.substring(file.text) == "// Not clamped yet" },
        )
    }

    @Test
    @DisplayName("Traditional Javadoc comments are rejected")
    fun rejectTraditionalJavadoc() {
        val copy = clampCopy("Clamps a value.")
        val changed = AnnotaterJavadocSourceCopy(
            copy.fileName,
            copy.text.replace("/// Clamps a value.", "/** Clamps a value. */"),
            copy.targets,
            copy.languageLevel,
        )
        val result = changed.extractJavadocs(changed.createPsiFile(project)!!)
        assertInstanceOf(AnnotaterJavadocSourceCopy.ExtractResult.Error::class.java, result)
    }

    private fun startEditing(copy: AnnotaterJavadocSourceCopy, target: AnnotaterJavadocTarget) =
        AnnotaterJavadocInlineEditor.start(
            project,
            AnnotaterJavadocVirtualFile(checkout, copy, target),
        )!!

    @Test
    @DisplayName("Editing the Javadoc saves it to the annotater checkout")
    fun save() {
        val inlineEditor = startEditing(clampCopy(null, placeholderFor = clamp), clamp)
        val document = FileDocumentManager.getInstance().getDocument(inlineEditor.file)!!
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                val offset = document.text.indexOf("/// ") + 4
                document.insertString(offset, "Clamps a value.")
            }
            assertTrue(inlineEditor.save().get(10, TimeUnit.SECONDS))
            assertEquals(
                "CLASS net/minecraft/util/Mth\n\tMETHOD clamp (III)I\n\t\tCOMMENT Clamps a value.\n",
                root.resolve("mappings/net/minecraft/util/Mth.mapping").readText(),
            )
        } finally {
            Disposer.dispose(inlineEditor)
        }
    }

    @Test
    @DisplayName("Only the edited Javadoc is editable")
    fun guards() {
        val inlineEditor = startEditing(clampCopy("Clamps a value."), clamp)
        val document = FileDocumentManager.getInstance().getDocument(inlineEditor.file)!!
        try {
            val text = document.text
            fun guarded(start: Int, end: Int) = document.getRangeGuard(start, end) != null
            val comment = text.indexOf("/// Clamps a value.")
            val commentEnd = comment + "/// Clamps a value.".length
            assertFalse(guarded(comment, commentEnd))
            assertTrue(guarded(commentEnd, commentEnd + 1), "The line break after the comment is read-only")
            assertTrue(guarded(text.indexOf("return value"), text.indexOf("return value") + 1))
            assertTrue(guarded(text.indexOf("PI"), text.indexOf("PI") + 1))
        } finally {
            Disposer.dispose(inlineEditor)
        }
    }

    @Test
    @DisplayName("Saving doesn't overwrite Javadocs of other members that changed on disk")
    fun saveOnlyEdited() {
        checkout.javadocStore.setAll(mapOf(pi to "Pi."))
        val copy = AnnotaterJavadocSourceCopy.create(findMember("net.minecraft.util.Mth", "clamp"), clamp) {
            checkout.javadocStore.get(it)
        }!!
        val inlineEditor = startEditing(copy, clamp)
        val document = FileDocumentManager.getInstance().getDocument(inlineEditor.file)!!
        try {
            checkout.javadocStore.setAll(mapOf(pi to "Changed on disk."))
            WriteCommandAction.runWriteCommandAction(project) {
                document.insertString(document.text.indexOf("/// \n    public static int") + 4, "Clamps a value.")
            }
            assertTrue(inlineEditor.save().get(10, TimeUnit.SECONDS))
            checkout.javadocStore.invalidate(clamp.outerClass)
            assertEquals("Clamps a value.", checkout.javadocStore.get(clamp))
            assertEquals("Changed on disk.", checkout.javadocStore.get(pi))
        } finally {
            Disposer.dispose(inlineEditor)
        }
    }

    @Test
    @DisplayName("The Javadoc is edited in an editor embedded in the class, which only shows the Javadoc")
    fun inlineEditor() {
        val method = findMember("net.minecraft.util.Mth", "clamp")
        fixture.openFileInEditor(method.containingFile.virtualFile)
        val host = fixture.editor as EditorEx
        val inlineEditor = startEditing(clampCopy("Clamps a value.\n\n@param value the value"), clamp)
        try {
            inlineEditor.show(host, AnnotaterJavadocRendering.displayOffset(method))
            val editor = inlineEditor.editor!!
            val document = editor.document
            // Only the 3 comment lines are visible
            assertEquals(2, editor.offsetToVisualLine(document.textLength, false))
            assertEquals(0, editor.offsetToVisualLine(0, false))
            assertEquals(
                document.text.indexOf("/// Clamps a value.") + "/// Clamps a value.".length,
                editor.caretModel.offset,
            )
            assertEquals(1, host.inlayModel.getBlockElementsInRange(0, host.document.textLength).size)

            // The end of the document is displayed at the end of the last comment line
            editor.caretModel.moveToOffset(document.textLength)
            val lastLine = document.text.indexOf("/// @param value the value") + "/// @param value the value".length
            assertEquals(lastLine, editor.caretModel.offset)
            editor.caretModel.moveToOffset(0)
            val firstLine = document.getLineStartOffset(document.getLineNumber(document.text.indexOf("/// Clamps")))
            assertEquals(firstLine, editor.caretModel.offset)

            WriteCommandAction.runWriteCommandAction(project) {
                document.insertString(lastLine, " Or not.")
            }
            val edited = document.text
            // Undo uses the embedded editor, not the host editor it's embedded in
            val dataManager = DataManager.getInstance() as HeadlessDataManager
            HeadlessDataManager.fallbackToProductionDataManager(inlineEditor)
            dataManager.setTestDataProvider(null, inlineEditor)
            val fileEditor = DataManager.getInstance().getDataContext(editor.contentComponent)
                .getData(PlatformCoreDataKeys.FILE_EDITOR) as TextEditor
            assertSame(editor, fileEditor.editor)
            val undoManager = UndoManager.getInstance(project)
            assertTrue(undoManager.isUndoAvailable(fileEditor))
            undoManager.undo(fileEditor)
            assertFalse(document.text.contains("Or not."))
            undoManager.redo(fileEditor)
            assertEquals(edited, document.text)

            assertTrue(inlineEditor.finish())
            assertNull(AnnotaterJavadocInlineEditor.current(project))
            assertTrue(host.inlayModel.getBlockElementsInRange(0, host.document.textLength).isEmpty())
        } finally {
            Disposer.dispose(inlineEditor)
        }
    }

    @Test
    @DisplayName("Editing can't be finished while the Javadoc can't be saved")
    fun finishInvalid() {
        val inlineEditor = startEditing(clampCopy("Clamps a value."), clamp)
        val document = FileDocumentManager.getInstance().getDocument(inlineEditor.file)!!
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                val start = document.text.indexOf("/// Clamps a value.")
                document.replaceString(start, start + "/// Clamps a value.".length, "/** Clamps a value. */")
            }
            assertFalse(inlineEditor.finish())
            assertSame(inlineEditor, AnnotaterJavadocInlineEditor.current(project))
            assertTrue(inlineEditor.finish(force = true))
            assertNull(AnnotaterJavadocInlineEditor.current(project))
        } finally {
            Disposer.dispose(inlineEditor)
        }
    }

    @Test
    @DisplayName("Intentions are only offered in the editable Javadocs")
    fun intentionFilter() {
        fixture.openFileInEditor(AnnotaterJavadocVirtualFile(checkout, clampCopy("Clamps a value."), clamp))
        val file = fixture.file
        val text = fixture.editor.document.text
        val intention = object : IntentionAction {
            override fun getText() = "Test"
            override fun getFamilyName() = text
            override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true
            override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {}
            override fun startInWriteAction() = true
        }
        val filter = AnnotaterJavadocIntentionActionFilter()
        assertTrue(filter.accept(intention, file, text.indexOf("Clamps")))
        assertFalse(filter.accept(intention, file, text.indexOf("return value")))
        assertFalse(filter.accept(intention, file, text.indexOf("// Not clamped")))
        assertFalse(filter.accept(intention, file, -1))
    }

    @Test
    @DisplayName("The documented declaration's intentions aren't offered in its Javadoc, unlike the Javadoc's own")
    fun declarationIntentions() {
        val javadocIntention = object : PsiElementBaseIntentionAction() {
            override fun getText() = "Javadoc intention"
            override fun getFamilyName() = text
            override fun isAvailable(project: Project, editor: Editor, element: PsiElement) =
                PsiTreeUtil.getParentOfType(element, PsiDocComment::class.java) != null
            override fun invoke(project: Project, editor: Editor, element: PsiElement) {}
        }
        IntentionManager.getInstance().addAction(javadocIntention)
        Disposer.register(fixture.testRootDisposable) {
            IntentionManager.getInstance().unregisterIntention(javadocIntention)
        }
        fixture.enableInspections(AnnotaterJavadocUnqualifiedReferenceInspection::class)
        fixture.openFileInEditor(AnnotaterJavadocVirtualFile(checkout, clampCopy("Clamps a [String] value."), clamp))
        val text = fixture.editor.document.text
        fixture.editor.caretModel.moveToOffset(text.indexOf("Clamps"))
        var intentions = fixture.availableIntentions.map { it.text }
        assertTrue("Javadoc intention" in intentions, intentions.toString())
        assertFalse("Change access modifier" in intentions, intentions.toString())
        assertFalse("Generate overloaded method with default parameter values" in intentions, intentions.toString())
        fixture.editor.caretModel.moveToOffset(text.indexOf("String]"))
        intentions = fixture.availableIntentions.map { it.text }
        assertTrue("Replace with 'java.lang.String'" in intentions, intentions.toString())
        assertFalse("Change access modifier" in intentions, intentions.toString())
    }

    @Test
    @DisplayName("Unqualified class references are reported and fixed")
    fun unqualifiedReferences() {
        val copy = clampCopy(
            "See [Mth], [net.minecraft.util.Mth#clamp], [#clamp] and [String].\n\n@see net.minecraft.world.level.Level",
        )
        fixture.enableInspections(AnnotaterJavadocUnqualifiedReferenceInspection::class)
        fixture.openFileInEditor(AnnotaterJavadocVirtualFile(checkout, copy, clamp))

        val warnings = fixture.doHighlighting(HighlightSeverity.WARNING)
            .filter { it.description?.contains("fully qualified") == true }
            .map { fixture.editor.document.getText(TextRange(it.startOffset, it.endOffset)) }
        assertEquals(listOf("Mth", "String"), warnings)

        fixture.editor.caretModel.moveToOffset(fixture.editor.document.text.indexOf("[Mth]") + 2)
        // launchAction can't be used, since it tries to make the light file read only on disk
        val fix = fixture.findSingleIntention("Replace with 'net.minecraft.util.Mth'")
        WriteCommandAction.runWriteCommandAction(project) { fix.invoke(project, fixture.editor, fixture.file) }
        assertTrue(fixture.editor.document.text.contains("See [net.minecraft.util.Mth], "))
    }

    @Test
    @DisplayName("Class name completion inserts fully qualified names")
    fun completion() {
        val copy = clampCopy("See [Lev].")
        fixture.openFileInEditor(AnnotaterJavadocVirtualFile(checkout, copy, clamp))
        fixture.editor.caretModel.moveToOffset(fixture.editor.document.text.indexOf("Lev]") + 3)
        val lookup = fixture.completeBasic()
        // java.util.logging.Level is also offered
        val level = lookup.single { (it.psiElement as? PsiClass)?.qualifiedName == "net.minecraft.world.level.Level" }
        fixture.lookup.currentItem = level
        fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
        assertTrue(
            fixture.editor.document.text.contains("/// See [net.minecraft.world.level.Level]."),
            fixture.editor.document.text,
        )
    }

    @Test
    @DisplayName("Method completion inserts fully qualified parameter types")
    fun methodParameterCompletion() {
        val copy = clampCopy("See [net.minecraft.world.level.Level#addEn].")
        fixture.openFileInEditor(AnnotaterJavadocVirtualFile(checkout, copy, clamp))
        fixture.editor.caretModel.moveToOffset(fixture.editor.document.text.indexOf("#addEn]") + 6)
        TemplateManagerImpl.setTemplateTesting(fixture.testRootDisposable)
        withAnnotaterCodeStyle { fixture.completeBasic() }
        // The parameters can still be edited with the template Java starts for them
        val templateState = TemplateManagerImpl.getTemplateState(fixture.editor)
        assertNotNull(templateState)
        val parameters = fixture.editor.document.getText(templateState!!.getVariableRange("PARAMETERS")!!)
        assertTrue(parameters.contains("net.minecraft.util.Ticker, java.lang.String\\[\\], int"), parameters)
        templateState.gotoEnd(false)
        val text = fixture.editor.document.text
        assertTrue(
            text.contains(
                "See [net.minecraft.world.level.Level#addEntity(net.minecraft.util.Ticker, java.lang.String\\[\\], int)].",
            ),
            text,
        )
    }

    @Test
    @DisplayName("Quick documentation shows the annotater Javadoc instead of the existing one")
    fun quickDocumentation() {
        val clampMethod = findMember("net.minecraft.util.Mth", "clamp")
        val doc = AnnotaterJavadocRendering.generateDoc(clampMethod, "Clamps a **value**.")!!
        assertTrue("Clamps a <b>value</b>." in doc || "Clamps a <strong>value</strong>." in doc, doc)
        assertFalse("Existing method docs" in doc, doc)

        val hidden = AnnotaterJavadocRendering.generateDoc(clampMethod, null)!!
        assertFalse("Existing method docs" in hidden, hidden)
    }

    @Test
    @DisplayName("Names that displayed Javadocs refer to are highlighted")
    fun referenceHighlights() {
        val javadoc = "Clamps a value.\n\nSee [#clamp(int, int, int)]."
        val highlights = AnnotaterJavadocRendering.highlightReferences(
            findMember("net.minecraft.util.Mth", "clamp"),
            { if (it == clamp) javadoc else null },
            EditorColorsManager.getInstance().globalScheme,
        )[clamp].orEmpty()
        val lines = javadoc.lines().map { if (it.isEmpty()) "///" else "/// $it" }
        assertEquals(
            listOf(2 to "clamp"),
            highlights.map { it.line to lines[it.line].substring(it.startColumn, it.endColumn) },
        )
    }

    // TODO: uncomment in 2026.3
    // @Test
    // @DisplayName("Inserting a Javadoc stub formats it")
    // fun javadocStub() {
    //     val inlineEditor = startEditing(clampCopy(null, placeholderFor = clamp), clamp)
    //     try {
    //         fixture.openFileInEditor(inlineEditor.file)
    //         fixture.editor.caretModel.moveToOffset(fixture.editor.document.text.indexOf("/// ") + 4)
    //         withAnnotaterCodeStyle { fixture.completeBasic() }
    //         fixture.lookup.currentItem = fixture.lookupElements!!.single { it.lookupString == "Insert Javadoc Stub" }
    //         withAnnotaterCodeStyle { fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR) }
    //         val text = fixture.editor.document.text
    //         assertTrue(
    //             text.contains(
    //                 "    /// \n    /// @param value\n    /// @param min\n    /// @param max\n    /// @return\n" +
    //                     "    public static int clamp(",
    //             ),
    //             text,
    //         )
    //     } finally {
    //         Disposer.dispose(inlineEditor)
    //     }
    // }

    @Test
    @DisplayName("Formatting only changes the edited Javadoc")
    fun formatting() {
        val addEntity = AnnotaterJavadocTarget.MethodTarget(
            "net/minecraft/world/level/Level",
            "addEntity",
            "(Lnet/minecraft/util/Ticker;[Ljava/lang/String;I)Z",
        )
        val copy = AnnotaterJavadocSourceCopy.create(findMember("net.minecraft.world.level.Level", "addEntity"), null) {
            if (it == addEntity) "Adds an entity." else null
        }!!
        val inlineEditor = startEditing(copy, addEntity)
        val document = FileDocumentManager.getInstance().getDocument(inlineEditor.file)!!
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                val offset = document.text.indexOf("/// Adds an entity.") + "/// Adds an entity.".length
                document.insertString(offset, "\n  /// More.")
                val documentManager = PsiDocumentManager.getInstance(project)
                documentManager.commitDocument(document)
                CodeStyleManager.getInstance(project).reformat(documentManager.getPsiFile(document)!!)
            }
            val text = document.text
            assertTrue(text.contains("    /// Adds an entity.\n    /// More.\n"), text)
            assertTrue(text.contains("return  true ;"), text)
        } finally {
            Disposer.dispose(inlineEditor)
        }
    }
}
