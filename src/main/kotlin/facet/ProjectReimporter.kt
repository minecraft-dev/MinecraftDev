/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
            log.error("Rejected refresh all projects: $error")
        }
    }
}
