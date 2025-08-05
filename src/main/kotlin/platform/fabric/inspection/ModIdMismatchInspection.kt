/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.inspection

import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class ModIdMismatchInspection : LocalInspectionTool() {

    override fun getStaticDescription() = "Checks for a mismatch between the mod id in fabric.mod.json and the java code."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitField(field: PsiField) {
            super.visitField(field)

            if (field.name != "MOD_ID" && field.name != "MODID") return
            val initializer = field.initializer
            val javaModId = initializer?.constantStringValue ?: return

            val project = field.project
            val jsonModId = getModIdFromJson(project, field.manager) ?: return

            if (javaModId != jsonModId) {
                holder.registerProblem(
                    initializer,
                    "Mod ID '$javaModId' does not match mod id '$jsonModId' from fabric.mod.json"
                )
            }
        }

        private fun getModIdFromJson(project: Project, manager: PsiManager): String? {
            val files = FilenameIndex.getVirtualFilesByName(
                "fabric.mod.json",
                GlobalSearchScope.projectScope(project)
            )

            val file = files.firstOrNull() ?: return null

            val jsonFile = manager.findFile(file) as? JsonFile ?: return null
            val topLevelObj = jsonFile.topLevelValue as? JsonObject ?: return null

            val stringLiteral = topLevelObj.findProperty("id")?.value as? JsonStringLiteral ?: return null
            return stringLiteral.value
        }
    }
}
