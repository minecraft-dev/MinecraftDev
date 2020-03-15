/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config.inspection

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.config.MixinConfigFileType
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

abstract class MixinConfigInspection : LocalInspectionTool() {

    protected abstract fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor

    private fun checkFile(file: PsiFile): Boolean {
        return file.fileType === MixinConfigFileType && MixinModuleType.isInModule(file)
    }

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (checkFile(holder.file)) {
            return buildVisitor(holder)
        }

        return PsiElementVisitor.EMPTY_VISITOR
    }

    final override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (checkFile(file)) {
            super.processFile(file, manager)
        } else {
            listOf()
        }
    }
}
