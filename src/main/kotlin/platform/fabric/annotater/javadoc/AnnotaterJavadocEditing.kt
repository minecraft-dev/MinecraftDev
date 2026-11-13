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
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterConfigurable
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterNotifications
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterSettings
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterVcs
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.util.PsiTreeUtil
import java.io.IOException

object AnnotaterJavadocEditing {
    /**
     * In Fabric projects it's offered even before annotater is set up, to make it discoverable.
     */
    fun isOffered(project: Project): Boolean {
        return AnnotaterSettings.instance.enabled || AnnotaterCheckoutService.isFabricProject(project)
    }

    /**
     * Finds the Minecraft class, field or method that the caret is on or in, which may be a reference to it.
     */
    fun findTarget(editor: Editor, file: PsiFile): PsiMember? {
        val referenced = TargetElementUtil.findTargetElement(
            editor,
            TargetElementUtil.ELEMENT_NAME_ACCEPTED or TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED,
        )
        val candidate = referenced?.let(::normalize)
            ?: file.findElementAt(editor.caretModel.offset)?.let { element ->
                PsiTreeUtil.getParentOfType(element, PsiMember::class.java, false)?.let(::normalize)
            }
            ?: return null
        return candidate.takeIf { isDocumentable(it) }
    }

    fun normalize(element: PsiElement): PsiMember? {
        val member = (element.getUserData(ClsElementImpl.COMPILED_ELEMENT) ?: element) as? PsiMember ?: return null
        return member.takeIf { AnnotaterJavadocTarget.fromPsi(it) != null }
    }

    private fun isDocumentable(member: PsiMember): Boolean {
        val file = AnnotaterCheckoutService.virtualFileOf(member) ?: return false
        return AnnotaterCheckoutService.isMinecraftFile(member.project, file)
    }

    fun open(project: Project, editor: Editor, file: PsiFile) {
        open(project, findTarget(editor, file) ?: return)
    }

    /**
     * Edits in an editor embedded in the Minecraft class [member] is displayed in.
     */
    fun open(project: Project, member: PsiMember) {
        val checkout = when (val status = AnnotaterCheckoutService.instance.status(member)) {
            is AnnotaterCheckoutService.Status.Available -> status.checkout
            AnnotaterCheckoutService.Status.Disabled -> {
                AnnotaterConfigurable.Util.show(project)
                return
            }

            else -> {
                AnnotaterNotifications.notifyUnavailable(project, status)
                return
            }
        }

        val pointer = SmartPointerManager.createPointer(member)
        val vcs = AnnotaterVcs.instance
        if (!checkout.managed || vcs == null) {
            openEditor(project, checkout, pointer.element ?: return)
            return
        }

        object : Task.Backgroundable(project, MCDevBundle("fabric.annotater.preparing"), true) {
            override fun run(indicator: ProgressIndicator) {
                vcs.prepareForEditing(project, checkout)
            }

            override fun onSuccess() {
                pointer.element?.let { openEditor(project, checkout, it) }
            }

            override fun onThrowable(error: Throwable) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.preparing.failed"), error)
            }
        }.queue()
    }

    private fun openEditor(project: Project, checkout: AnnotaterCheckout, member: PsiMember) {
        val target = AnnotaterJavadocTarget.fromPsi(member) ?: return
        AnnotaterJavadocInlineEditor.current(project)?.let { current ->
            if (current.target == target && current.checkout.root == checkout.root && current.editor != null) {
                current.editor?.let { IdeFocusManager.getInstance(project).requestFocus(it.contentComponent, true) }
                return
            }
        }

        val (host, hostMember) = findHost(project, member, target) ?: return
        val copy = try {
            ReadAction.computeBlocking<AnnotaterJavadocSourceCopy?, IOException> {
                AnnotaterJavadocSourceCopy.create(member, placeholderFor = target) { checkout.javadocStore.get(it) }
            }
        } catch (e: IOException) {
            AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.javadoc.open_failed"), e)
            return
        } ?: return

        val file = AnnotaterJavadocVirtualFile(checkout, copy, target)
        val inlineEditor =
            AnnotaterJavadocInlineEditor.start(project, file, SmartPointerManager.createPointer(hostMember)) ?: return
        inlineEditor.show(host, AnnotaterJavadocRendering.displayOffset(hostMember))
    }

    /**
     * Finds the editor that displays the top level class of [member], opening it if necessary, and the member that
     * documents [target] in it.
     */
    private fun findHost(
        project: Project,
        member: PsiMember,
        target: AnnotaterJavadocTarget,
    ): Pair<EditorEx, PsiMember>? {
        fun findIn(editor: Editor?): Pair<EditorEx, PsiMember>? {
            if (editor !is EditorEx) {
                return null
            }
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? PsiJavaFile
                ?: return null
            val hostMember = AnnotaterJavadocTarget.declarations(file)
                .firstOrNull { AnnotaterJavadocTarget.fromPsi(it) == target }
                ?: return null
            return editor to hostMember
        }

        val editorManager = FileEditorManager.getInstance(project)
        findIn(editorManager.selectedTextEditor)?.let { return it }
        member.navigate(true)
        return findIn(editorManager.selectedTextEditor)
    }
}
