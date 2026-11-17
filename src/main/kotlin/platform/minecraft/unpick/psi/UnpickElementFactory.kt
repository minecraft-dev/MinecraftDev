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

package com.demonwav.mcdev.platform.minecraft.unpick.psi

import com.demonwav.mcdev.platform.minecraft.unpick.UnpickFile
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickFileType
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickClassName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTargetAnnotation
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object UnpickElementFactory {
    private fun createFile(project: Project, text: String): UnpickFile {
        return PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.unpick", UnpickFileType, text) as UnpickFile
    }

    fun createIdentifier(project: Project, name: String): PsiElement {
        val annotation = PsiTreeUtil.findChildOfType(
            createFile(project, "unpick v4\ntarget_annotation a.A $name"),
            UnpickTargetAnnotation::class.java,
        )!!
        return annotation.groupRef!!.node.findChildByType(UnpickTypes.IDENTIFIER)!!.psi
    }

    fun createClassName(project: Project, name: String): UnpickClassName {
        val annotation = PsiTreeUtil.findChildOfType(
            createFile(project, "unpick v4\ntarget_annotation $name G"),
            UnpickTargetAnnotation::class.java,
        )!!
        return annotation.className!!
    }
}
