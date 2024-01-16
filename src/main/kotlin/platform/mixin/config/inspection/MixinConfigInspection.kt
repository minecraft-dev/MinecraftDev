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
