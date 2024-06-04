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

package com.demonwav.mcdev.creator.custom.finalizers

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlin.io.path.absolutePathString
import org.jetbrains.plugins.gradle.service.project.open.canLinkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject

class ImportGradleProjectFinalizer : CreatorFinalizer {

    override fun execute(project: Project, properties: Map<String, Any>, templateProperties: Map<String, Any?>) {
        val projectDir = project.guessProjectDir()?.toNioPath()?.absolutePathString()
            ?: return
        val canLink = canLinkAndRefreshGradleProject(projectDir, project, showValidationDialog = false)
        thisLogger().info("canLink = $canLink projectDir = $projectDir")
        if (canLink) {
            linkAndRefreshGradleProject(projectDir, project)
            thisLogger().info("Linking done")
        }
    }
}
