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

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.platform.forge.inspections.sideonly.SideOnlyUtil
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.runGradleTaskWithCallback
import com.demonwav.mcdev.util.runWriteActionAndWait
import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.ActionCallback
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyLibraryEntity
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import java.nio.file.Paths
import org.jetbrains.plugins.gradle.util.GradleUtil

class FabricLoomDecompileSourceProvider : AttachSourcesProvider {
    @Deprecated("Deprecated in AttachSourcesProvider")
    override fun getActions(
        orderEntries: List<LibraryOrderEntry>,
        psiFile: PsiFile,
    ): Collection<AttachSourcesProvider.AttachSourcesAction> {
        return getDecompileActions(psiFile)
    }

    override fun getLibrariesActions(
        libraryEntities: Collection<LibraryEntity?>,
        psiFile: PsiFile
    ): Collection<AttachSourcesProvider.AttachSourcesAction?> {
        return getDecompileActions(psiFile)
    }

    private fun getDecompileActions(psiFile: PsiFile): Collection<AttachSourcesProvider.AttachSourcesAction> {
        if (psiFile !is PsiJavaFile || !psiFile.packageName.startsWith("net.minecraft")) {
            return emptyList()
        }

        val module = psiFile.findModule() ?: return emptyList()
        val loomData = GradleUtil.findGradleModuleData(module)?.children
            ?.find { it.key == FabricLoomData.KEY }?.data as? FabricLoomData
            ?: return emptyList()

        val env = if (!loomData.splitMinecraftJar) {
            "single"
        } else if (isClientClass(psiFile)) {
            "client"
        } else {
            "common"
        }

        val decompileTasks = loomData.decompileTasks[env] ?: return emptyList()
        return decompileTasks.map(::DecompileAction)
    }

    private fun isClientClass(psiFile: PsiJavaFile): Boolean {
        return psiFile.classes.any { psiClass ->
            return SideOnlyUtil.getSideForClass(psiClass).second == Side.CLIENT
        }
    }

    private class DecompileAction(val decompiler: FabricLoomData.Decompiler) :
        AttachSourcesProvider.AttachSourcesAction {

        override fun getName(): String = "Decompile with ${decompiler.name}"
        @Suppress("DialogTitleCapitalization") // Minecraft is a proper noun
        override fun getBusyText(): String = "Decompiling Minecraft..."

        @Deprecated("Deprecated in AttachSourcesAction")
        override fun perform(orderEntriesContainingFile: List<LibraryOrderEntry>): ActionCallback {
            val project = orderEntriesContainingFile.firstOrNull()?.ownerModule?.project
                ?: return ActionCallback.REJECTED
            return performInternal(project) {
                attachSources(orderEntriesContainingFile, decompiler.sourcesPath)
            }
        }

        override fun perform(
            libraryEntities: Collection<LibraryEntity>,
            project: Project
        ): ActionCallback {
            return performInternal(project) {
                attachSources(project, libraryEntities, decompiler.sourcesPath)
            }
        }

        private fun performInternal(project: Project, attachSources: () -> ActionCallback): ActionCallback {
            val projectPath = project.basePath ?: return ActionCallback.REJECTED

            val callback = ActionCallback()
            val taskCallback = object : TaskCallback {
                override fun onSuccess() {
                    val innerCallback = attachSources()
                    innerCallback.doWhenProcessed {
                        if (innerCallback.isDone) {
                            callback.setDone()
                        } else {
                            callback.setRejected()
                        }
                    }
                }

                override fun onFailure() = callback.setRejected()
            }
            runGradleTaskWithCallback(
                project,
                Paths.get(projectPath),
                toolWindow = false,
                { settings -> settings.taskNames = listOf(decompiler.taskName) },
                taskCallback,
            )
            return callback
        }

        private fun attachSources(libraryEntries: List<LibraryOrderEntry>, sourcePath: String): ActionCallback {
            // Distinct because for some reason the same library is in there twice
            for (libraryEntry in libraryEntries.distinctBy { it.libraryName }) {
                val library = libraryEntry.library
                if (library != null) {
                    runWriteActionAndWait {
                        if (library is LibraryEx && library.isDisposed) {
                            return@runWriteActionAndWait
                        }

                        val model = library.modifiableModel
                        model.addRoot("jar://$sourcePath!/", OrderRootType.SOURCES)
                        model.commit()
                    }
                }
            }

            return ActionCallback.DONE
        }

        private fun attachSources(project: Project, libraryEntities: Collection<LibraryEntity>, sourcePath: String): ActionCallback {
            runWriteActionAndWait {
                val model = WorkspaceModel.getInstance(project)
                model.updateProjectModel("Attaching sources") { storage ->
                    for (libraryEntity in libraryEntities.distinctBy { it.name }) {
                        storage.modifyLibraryEntity(libraryEntity) {
                            roots += LibraryRoot(
                                model.getVirtualFileUrlManager().getOrCreateFromUrl("jar://$sourcePath!/"),
                                LibraryRootTypeId.SOURCES
                            )
                        }
                    }
                }
            }
            return ActionCallback.DONE
        }
    }
}
