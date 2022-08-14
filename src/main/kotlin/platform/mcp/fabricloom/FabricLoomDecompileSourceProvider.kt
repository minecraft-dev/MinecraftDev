/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.platform.forge.inspections.sideonly.SideOnlyUtil
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.runGradleTaskWithCallback
import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.ActionCallback
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import java.nio.file.Paths
import org.jetbrains.plugins.gradle.util.GradleUtil

class FabricLoomDecompileSourceProvider : AttachSourcesProvider {
    override fun getActions(
        orderEntries: List<LibraryOrderEntry>,
        psiFile: PsiFile
    ): Collection<AttachSourcesProvider.AttachSourcesAction> {
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

        override fun getBusyText(): String = "Decompiling Minecraft..."

        override fun perform(orderEntriesContainingFile: List<LibraryOrderEntry>): ActionCallback {
            val project = orderEntriesContainingFile.firstOrNull()?.ownerModule?.project
                ?: return ActionCallback.REJECTED
            val projectPath = project.basePath ?: return ActionCallback.REJECTED

            val callback = ActionCallback()
            val taskCallback = object : TaskCallback {
                override fun onSuccess() {
                    attachSources(orderEntriesContainingFile, decompiler.sourcesPath)
                    callback.setDone()
                }

                override fun onFailure() = callback.setRejected()
            }
            runGradleTaskWithCallback(
                project,
                Paths.get(projectPath),
                { settings -> settings.taskNames = listOf(decompiler.taskName) },
                taskCallback
            )
            return callback
        }

        private fun attachSources(libraryEntries: List<LibraryOrderEntry>, sourcePath: String): ActionCallback? {
            // Distinct because for some reason the same library is in there twice
            for (libraryEntry in libraryEntries.distinctBy { it.libraryName }) {
                val library = libraryEntry.library
                if (library != null) {
                    runWriteActionAndWait {
                        val model = library.modifiableModel
                        model.addRoot("jar://$sourcePath!/", OrderRootType.SOURCES)
                        model.commit()
                    }
                }
            }

            return ActionCallback.DONE
        }
    }
}
