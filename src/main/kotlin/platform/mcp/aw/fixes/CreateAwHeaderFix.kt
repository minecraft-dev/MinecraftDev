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

package com.demonwav.mcdev.platform.mcp.aw.fixes

import com.demonwav.mcdev.platform.mcp.aw.AwElementFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project

class CreateAwHeaderFix : LocalQuickFix {

    override fun getFamilyName(): @IntentionFamilyName String = "Create header"

    override fun getName(): @IntentionName String = familyName

    override fun startInWriteAction(): Boolean = true

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        var toInsert = AwElementFactory.createFile(project, "accessWidener v2 named\n\n")
        descriptor.psiElement.containingFile.addRangeAfter(toInsert.firstChild, toInsert.lastChild, null)
    }
}
