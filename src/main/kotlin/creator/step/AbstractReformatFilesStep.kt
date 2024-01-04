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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.notifyCreatedProjectNotOpened
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.nio.file.Path

abstract class AbstractReformatFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description
        get() = MCDevBundle("creator.step.reformat.description")

    private val filesToReformat = mutableListOf<String>()

    fun addFileToReformat(file: String) {
        filesToReformat += file
    }

    abstract fun addFilesToReformat()

    override fun perform(project: Project) {
        addFilesToReformat()

        val rootDir = VfsUtil.findFile(Path.of(context.projectFileDirectory), true) ?: return
        val psiManager = PsiManager.getInstance(project)
        val files = ReadAction.compute<Array<PsiFile>, Throwable> {
            filesToReformat.mapNotNull { path ->
                VfsUtil.findRelativeFile(rootDir, *path.split('/').toTypedArray())?.let(psiManager::findFile)
            }.toTypedArray()
        }
        files.ifEmpty { return }

        NonProjectFileWritingAccessProvider.disableChecksDuring {
            WriteCommandAction.writeCommandAction(project, *files).withGlobalUndo().run<Throwable> {
                if (project.isDisposed || !project.isInitialized) {
                    notifyCreatedProjectNotOpened()
                    return@run
                }

                ReformatCodeProcessor(project, files, null, false).run()
            }
        }
    }
}
