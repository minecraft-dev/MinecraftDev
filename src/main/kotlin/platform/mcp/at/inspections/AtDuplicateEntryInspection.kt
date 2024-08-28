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

package com.demonwav.mcdev.platform.mcp.at.inspections

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtVisitor
import com.demonwav.mcdev.util.childrenOfType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class AtDuplicateEntryInspection : LocalInspectionTool() {

    override fun getStaticDescription(): String? = "Reports duplicate AT entries in the same file"

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor = object : AtVisitor() {

        override fun visitEntry(entry: AtEntry) {
            // Either a MemberReference or the class name text for class-level entries
            val entryMemberReference = entry.memberReference ?: entry.className.text
            val allMemberReferences = entry.containingFile.childrenOfType<AtEntry>()
                .map { it.memberReference ?: it.className.text }
            if (allMemberReferences.count { it == entryMemberReference } > 1) {
                holder.registerProblem(entry, "Duplicate entry", RemoveAtEntryFix.forWholeLine(entry, false))
            }
        }
    }
}
