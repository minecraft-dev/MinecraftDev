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

package com.demonwav.mcdev.platform.mcp.vanillagradle

import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.runGradleTaskWithCallback
import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.util.ActionCallback
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import java.nio.file.Paths
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil

class VanillaGradleDecompileSourceProvider : AttachSourcesProvider {
    override fun getActions(
        orderEntries: List<LibraryOrderEntry>,
        psiFile: PsiFile,
    ): Collection<AttachSourcesProvider.AttachSourcesAction> {
        if (psiFile !is PsiJavaFile || !psiFile.packageName.startsWith("net.minecraft")) {
            return emptyList()
        }

        val module = psiFile.findModule() ?: return emptyList()
        val vgData = GradleUtil.findGradleModuleData(module)?.children
            ?.find { it.key == VanillaGradleData.KEY }?.data as? VanillaGradleData
            ?: return emptyList()
        return listOf(DecompileAction(vgData.decompileTaskName))
    }

    private class DecompileAction(val decompileTaskName: String) : AttachSourcesProvider.AttachSourcesAction {

        override fun getName(): String = "Decompile Minecraft"

        override fun getBusyText(): String = "Decompiling Minecraft..."

        override fun perform(orderEntriesContainingFile: List<LibraryOrderEntry>): ActionCallback {
            val project = orderEntriesContainingFile.firstOrNull()?.ownerModule?.project
                ?: return ActionCallback.REJECTED
            val projectPath = project.basePath ?: return ActionCallback.REJECTED

            val callback = ActionCallback()
            val taskCallback = object : TaskCallback {
                override fun onSuccess() {
                    val importSpec = ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                        .callback(
                            object : ExternalProjectRefreshCallback {
                                override fun onSuccess(externalProject: DataNode<ProjectData>?) = callback.setDone()

                                override fun onFailure(errorMessage: String, errorDetails: String?) {
                                    callback.reject(
                                        if (errorDetails == null) errorMessage else "$errorMessage: $errorDetails",
                                    )
                                }
                            },
                        )
                    ExternalSystemUtil.refreshProject(projectPath, importSpec)
                }

                override fun onFailure() = callback.setRejected()
            }
            runGradleTaskWithCallback(
                project,
                Paths.get(projectPath),
                { settings -> settings.taskNames = listOf(decompileTaskName) },
                taskCallback,
            )
            return callback
        }
    }
}
