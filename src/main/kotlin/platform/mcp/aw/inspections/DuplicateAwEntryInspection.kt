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

package com.demonwav.mcdev.platform.mcp.aw.inspections

import com.demonwav.mcdev.platform.mcp.aw.AwFile
import com.demonwav.mcdev.platform.mcp.aw.fixes.RemoveAwEntryFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile

class DuplicateAwEntryInspection : LocalInspectionTool() {

    override fun runForWholeFile(): Boolean = true

    override fun getDisplayName(): String = "Duplicate AW entry"

    override fun getStaticDescription(): String = "Warns when the same element has its accessibility, mutability, " +
        "or extensibility changed multiple times in one file."

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        return (file as AwFile).entries
            .groupBy { it.accessKind to it.memberReference }
            .filter { (key, matches) -> key.second != null && matches.size > 1 }
            .flatMap { (_, matches) ->
                matches.asSequence().map { match ->
                    manager.createProblemDescriptor(
                        match,
                        "Duplicate entry",
                        RemoveAwEntryFix.forWholeLine(match, false),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                    )
                }
            }.toTypedArray()
    }
}
