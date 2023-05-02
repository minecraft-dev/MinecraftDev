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

package com.demonwav.mcdev.platform.forge.inspections.simpleimpl

import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.siyeh.ig.InspectionGadgetsFix

object AddEmptyConstructorInspectionGadgetsFix : InspectionGadgetsFix() {

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val clazz = descriptor.psiElement.findContainingClass() ?: return
        clazz.addBefore(JavaPsiFacade.getElementFactory(project).createConstructor(), clazz.methods[0])
    }

    override fun getName() = "Add empty constructor"

    override fun getFamilyName() = name
}
