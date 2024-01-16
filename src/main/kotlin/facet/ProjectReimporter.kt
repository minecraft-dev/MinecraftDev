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

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.project.trusted.ExternalSystemTrustedProjectDialog
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
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

    suspend fun reimport(project: Project) {
        for (module in ModuleManager.getInstance(project).modules) {
            val minecraftFacet = MinecraftFacet.getInstance(module) ?: continue
            minecraftFacet.configuration.state.projectReimportVersion = CURRENT_REIMPORT_VERSION
        }

        val systemIds = getSystemIds()
        if (systemIds.isEmpty()) {
            log.error("No external system found")
            return
        }

        // We save all documents because there is a possible case that there is an external system config file changed inside the ide.
        FileDocumentManager.getInstance().saveAllDocuments()

        for (externalSystemId in systemIds) {
            if (ExternalSystemTrustedProjectDialog.confirmLoadingUntrustedProjectAsync(project, externalSystemId)) {
                ExternalSystemUtil.refreshProjects(ImportSpecBuilder(project, externalSystemId))
            }
        }
    }

    private fun getSystemIds(): List<ProjectSystemId> {
        val systemIds: MutableList<ProjectSystemId> = ArrayList()
        ExternalSystemManager.EP_NAME.forEachExtensionSafe { manager: ExternalSystemManager<*, *, *, *, *> ->
            systemIds.add(manager.systemId)
        }
        return systemIds
    }
}
