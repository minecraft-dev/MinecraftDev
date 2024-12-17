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

import com.demonwav.mcdev.platform.mcp.at.inspections.AtUsageInspection
import com.demonwav.mcdev.platform.mcp.aw.AwFileType
import com.demonwav.mcdev.platform.mcp.aw.fixes.RemoveAwEntryFix
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwClassEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwFieldEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwMethodEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class AwUsageInspection : LocalInspectionTool() {

    override fun getStaticDescription(): String {
        return "Reports unused Access Widener entries"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : AwVisitor() {

            private val fixProvider = { it: AwEntry -> RemoveAwEntryFix.forWholeLine(it, true) }

            override fun visitClassEntry(entry: AwClassEntry) {
                entry.className?.let { AtUsageInspection.checkElement(entry, it, holder, AwFileType, fixProvider) }
            }

            override fun visitFieldEntry(entry: AwFieldEntry) {
                entry.memberName?.let { AtUsageInspection.checkElement(entry, it, holder, AwFileType, fixProvider) }
            }

            override fun visitMethodEntry(entry: AwMethodEntry) {
                entry.memberName?.let { memberName ->
                    AtUsageInspection.checkElement(entry, memberName, holder, AwFileType, fixProvider) { file, toSkip ->
                        file.children.asSequence()
                            .filterIsInstance<AwMethodEntry>()
                            .filter { it != toSkip }
                            .mapNotNull { it.memberName?.reference }
                    }
                }
            }
        }
    }
}
