/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

/**
 * Forces a project reimport on existing projects if a new MinecraftDev version requires it to add extra setup to the
 * project.
 */
object ProjectReimporter {
    private val log = logger<ProjectReimporter>()

    const val CURRENT_REIMPORT_VERSION = 1

    fun needsReimport(facet: MinecraftFacet) =
        facet.configuration.state.projectReimportVersion < CURRENT_REIMPORT_VERSION

    fun reimport(project: Project) {
        for (module in ModuleManager.getInstance(project).modules) {
            val minecraftFacet = MinecraftFacet.getInstance(module) ?: continue
            minecraftFacet.configuration.state.projectReimportVersion = CURRENT_REIMPORT_VERSION
        }

        val refreshAllProjectsAction = ActionManager.getInstance().getAction("ExternalSystem.RefreshAllProjects")
        if (refreshAllProjectsAction == null) {
            log.error("Could not find refresh all projects action")
            return
        }
        val callback = ActionManager.getInstance().tryToExecute(
            refreshAllProjectsAction,
            null,
            null,
            ActionPlaces.UNKNOWN,
            true
        )
        callback.doWhenRejected { error ->
            if (error == null) {
                Notification(
                    "Minecraft facet",
                    MCDevBundle("facet.reimport.failed.title"),
                    MCDevBundle("facet.reimport.failed.content.no_error"),
                    NotificationType.WARNING
                ).notify(project)
                log.warn("Rejected refresh all projects, no details provided")
            } else {
                Notification(
                    "Minecraft facet",
                    MCDevBundle("facet.reimport.failed.title"),
                    MCDevBundle("facet.reimport.failed.content.with_error", error),
                    NotificationType.WARNING
                ).notify(project)
                log.error("Rejected refresh all projects: $error")
            }
        }
    }
}
