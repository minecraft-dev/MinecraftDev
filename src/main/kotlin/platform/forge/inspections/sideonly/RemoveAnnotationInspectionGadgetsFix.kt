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

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.util.findAnnotation
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.findParentOfType
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class RemoveAnnotationInspectionGadgetsFix(
    private val annotationName: String,
    private val name: String
) : InspectionGadgetsFix() {

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val decl = descriptor.psiElement.findParentOfType<PsiModifierListOwner>() ?: return
        decl.findAnnotation(annotationName)?.delete()
    }

    @Nls
    override fun getName() = name

    @Nls
    override fun getFamilyName() = name
}
