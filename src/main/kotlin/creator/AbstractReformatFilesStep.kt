/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.runWriteTask
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.nio.file.Path

abstract class AbstractReformatFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Reformatting files"

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

        runWriteTask {
            WriteCommandAction.writeCommandAction(project, *files).withGlobalUndo().run<Throwable> {
                ReformatCodeProcessor(project, files, null, false).run()
            }
        }
    }
}
