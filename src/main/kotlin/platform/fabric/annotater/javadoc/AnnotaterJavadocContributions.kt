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
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterConfigurable
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterContribution
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterNotifications
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterVcs
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

/**
 * Switching a managed annotater checkout between contributions, so that a pull request can be updated after review.
 */
object AnnotaterJavadocContributions {
    /**
     * The label for the contribution [checkout] is on, for the editor banner.
     */
    fun currentLabel(vcs: AnnotaterVcs, checkout: AnnotaterCheckout): String =
        vcs.currentContribution(checkout)?.displayName ?: MCDevBundle("fabric.annotater.contribution.new")

    /**
     * Shows a popup under [component] for switching between contributions if [checkout] is managed, and configuring
     * annotater.
     */
    fun showPopup(project: Project, checkout: AnnotaterCheckout, component: JComponent) {
        val vcs = AnnotaterVcs.instance?.takeIf { checkout.managed }
        if (vcs == null) {
            showPopup(project, checkout, component, null, null)
            return
        }
        ApplicationManager.getApplication().executeOnPooledThread {
            val contributions = try {
                vcs.contributions(project, checkout)
            } catch (e: Exception) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.contribution.failed"), e)
                return@executeOnPooledThread
            }
            ApplicationManager.getApplication().invokeLater {
                showPopup(project, checkout, component, vcs, contributions)
            }
        }
    }

    private fun showPopup(
        project: Project,
        checkout: AnnotaterCheckout,
        component: JComponent,
        vcs: AnnotaterVcs?,
        contributions: List<AnnotaterContribution>?,
    ) {
        if (!component.isShowing) {
            return
        }
        val group = DefaultActionGroup()
        if (vcs != null && contributions != null) {
            val current = vcs.currentContribution(checkout)?.branch
            for (contribution in contributions) {
                group.add(SwitchAction(project, checkout, contribution, contribution.branch == current))
            }
            group.add(
                DumbAwareAction.create(MCDevBundle("fabric.annotater.contribution.new"), AllIcons.General.Add) {
                    switchTo(project, checkout, null)
                },
            )
            group.add(Separator.getInstance())
        }
        group.add(
            DumbAwareAction.create(MCDevBundle("fabric.annotater.configure"), AllIcons.General.Settings) {
                AnnotaterConfigurable.show(project)
            },
        )
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null,
                group,
                DataContext.EMPTY_CONTEXT,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true,
            )
            .showUnderneathOf(component)
    }

    private class SwitchAction(
        private val project: Project,
        private val checkout: AnnotaterCheckout,
        private val contribution: AnnotaterContribution,
        private val current: Boolean,
    ) : DumbAwareAction(
        contribution.displayName,
        contribution.branch,
        if (current) AllIcons.Actions.Checked else AllIcons.Vcs.Branch,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            if (!current) {
                switchTo(project, checkout, contribution.branch)
            }
        }
    }

    /**
     * Switches [checkout] to the contribution [branch], or to a new contribution if it's null. The Javadoc being
     * edited is saved first, any uncommitted changes are committed to the current contribution, and editing is then
     * restarted with the Javadoc from the new branch.
     */
    fun switchTo(project: Project, checkout: AnnotaterCheckout, branch: String?) {
        val vcs = AnnotaterVcs.instance ?: return
        val saves = listOfNotNull(javadocEditor(project, checkout)?.save())
        CompletableFuture.allOf(*saves.toTypedArray()).thenAccept {
            ApplicationManager.getApplication().invokeLater {
                if (!saves.all { it.join() }) {
                    Messages.showErrorDialog(
                        project,
                        MCDevBundle("fabric.annotater.contribution.save_failed"),
                        MCDevBundle("fabric.annotater.contribution.failed"),
                    )
                    return@invokeLater
                }
                checkUncommittedChanges(project, checkout, vcs, branch)
            }
        }
    }

    private fun checkUncommittedChanges(
        project: Project,
        checkout: AnnotaterCheckout,
        vcs: AnnotaterVcs,
        branch: String?,
    ) {
        object : Task.Backgroundable(project, MCDevBundle("fabric.annotater.contribution.switching"), true) {
            private var uncommitted = emptyList<String>()

            override fun run(indicator: ProgressIndicator) {
                uncommitted = vcs.uncommittedMappingFiles(project, checkout)
            }

            override fun onSuccess() {
                if (uncommitted.isEmpty()) {
                    switch(project, checkout, vcs, branch, null)
                    return
                }
                val current = vcs.currentContribution(checkout)?.displayName
                    ?: MCDevBundle("fabric.annotater.contribution.new")
                val message = Messages.showInputDialog(
                    project,
                    MCDevBundle("fabric.annotater.contribution.commit", current),
                    MCDevBundle("fabric.annotater.contribution.switch"),
                    null,
                    AnnotaterContribution.defaultTitle(uncommitted),
                    null,
                )?.takeIf { it.isNotBlank() } ?: return
                switch(project, checkout, vcs, branch, message)
            }

            override fun onThrowable(error: Throwable) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.contribution.failed"), error)
            }
        }.queue()
    }

    private fun switch(
        project: Project,
        checkout: AnnotaterCheckout,
        vcs: AnnotaterVcs,
        branch: String?,
        commitMessage: String?,
    ) {
        // The Javadoc being edited is from the old branch, so stop editing it and restart after switching
        val javadocEditor = javadocEditor(project, checkout)
        val reopen = javadocEditor?.source
        javadocEditor?.finish(force = true)

        object : Task.Backgroundable(project, MCDevBundle("fabric.annotater.contribution.switching"), false) {
            override fun run(indicator: ProgressIndicator) {
                vcs.switchContribution(project, checkout, branch, commitMessage, indicator)
            }

            override fun onThrowable(error: Throwable) {
                AnnotaterNotifications.notifyError(project, MCDevBundle("fabric.annotater.contribution.failed"), error)
            }

            override fun onFinished() {
                reopen?.element?.let { AnnotaterJavadocEditing.open(project, it) }
            }
        }.queue()
    }

    private fun javadocEditor(project: Project, checkout: AnnotaterCheckout): AnnotaterJavadocInlineEditor? =
        AnnotaterJavadocInlineEditor.current(project)?.takeIf { it.checkout.root == checkout.root }
}
