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

import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterListener
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotifications
import java.nio.file.Path

/**
 * Refreshes the places annotater Javadocs are displayed when they change, whether through the plugin or on disk.
 */
class AnnotaterJavadocRefresher : AnnotaterListener, BulkFileListener {
    override fun checkoutsChanged() {
        refresh(updateNotifications = true)
    }

    override fun javadocsChanged(checkoutRoot: Path, outerClass: String) {
        refresh()
    }

    override fun after(events: List<VFileEvent>) {
        val paths = events.mapNotNull { event ->
            val path = event.path
            path.takeIf { it.endsWith(".mapping") || it.endsWith("/config.json") }?.let { Path.of(it) }
        }
        if (paths.isEmpty()) {
            return
        }
        val (changedJavadocs, configChanged) = AnnotaterCheckoutService.instance.filesChanged(paths)
        val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC)
        if (configChanged) {
            publisher.checkoutsChanged()
        }
        for ((root, outerClass) in changedJavadocs) {
            publisher.javadocsChanged(root, outerClass)
        }
    }

    private fun refresh(updateNotifications: Boolean = false) {
        ApplicationManager.getApplication().invokeLater {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) {
                    DaemonCodeAnalyzer.getInstance(project).restart(this)
                    if (updateNotifications) {
                        // The banner is shown wherever there's a checkout
                        EditorNotifications.getInstance(project).updateAllNotifications()
                    }
                }
            }
        }
    }
}
