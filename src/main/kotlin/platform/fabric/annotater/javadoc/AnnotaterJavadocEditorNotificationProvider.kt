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
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterPullRequests
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterVcs
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.HyperlinkLabel
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import javax.swing.JComponent

class AnnotaterJavadocEditorNotificationProvider : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        val checkout = AnnotaterCheckoutService.instance.checkoutFor(project, file) ?: return null
        val javadocEditor = AnnotaterJavadocInlineEditor.current(project)?.takeIf { it.checkout.root == checkout.root }
        val error = javadocEditor?.file?.error
        val pullRequests = AnnotaterPullRequests.instance

        return Function { fileEditor ->
            val status =
                if (error != null) EditorNotificationPanel.Status.Error else EditorNotificationPanel.Status.Info
            val panel = EditorNotificationPanel(fileEditor, status)
            panel.text = error ?: MCDevBundle("fabric.annotater.banner", checkout.version, checkout.root)
            if (javadocEditor != null) {
                panel.createActionLabel(MCDevBundle("fabric.annotater.javadoc.finish")) {
                    javadocEditor.finish()
                }
            }

            val pullRequestAction = pullRequests?.availableAction(checkout)
            if (pullRequests != null && pullRequestAction != null) {
                val text = when (pullRequestAction) {
                    AnnotaterPullRequests.Action.CREATE -> MCDevBundle("fabric.annotater.create_pull_request")
                    AnnotaterPullRequests.Action.UPDATE -> MCDevBundle("fabric.annotater.update_pull_request")
                }
                panel.createActionLabel(text) {
                    val saved = javadocEditor?.save() ?: CompletableFuture.completedFuture(true)
                    saved.thenAccept { success ->
                        if (success) {
                            ApplicationManager.getApplication().invokeLater {
                                pullRequests.submit(project, checkout)
                            }
                        }
                    }
                }
            }
            // Switching contributions and configuring annotater are in one dropdown, to keep the banner short
            val vcs = AnnotaterVcs.instance
            val labelText = if (checkout.managed && vcs != null) {
                AnnotaterJavadocContributions.currentLabel(vcs, checkout)
            } else {
                MCDevBundle("fabric.annotater.menu")
            }
            lateinit var label: HyperlinkLabel
            label = panel.createActionLabel(MCDevBundle("fabric.annotater.dropdown", labelText)) {
                AnnotaterJavadocContributions.showPopup(project, checkout, label)
            }
            if (checkout.managed && vcs != null) {
                label.setIcon(AllIcons.Vcs.Branch)
                label.toolTipText = MCDevBundle("fabric.annotater.contribution.tooltip")
            }
            panel
        }
    }
}
