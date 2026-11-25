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

package com.demonwav.mcdev.platform.minecraft.unpick.inspections

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickGroups
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class UnpickGroupTypeConflictInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : UnpickVisitor() {
            override fun visitGroupDefinition(element: UnpickGroupDefinition) {
                val name = element.name ?: return
                val dataTypeElement = element.dataType ?: return
                val dataType = dataTypeElement.dataType ?: return
                val otherTypes = UnpickGroups.findDefinitions(element, name)
                    .mapNotNull { it.dataType?.dataType }
                    .filter { it != dataType }
                    .distinct()
                if (otherTypes.isNotEmpty()) {
                    holder.registerProblem(
                        dataTypeElement,
                        MCDevBundle(
                            "inspection.unpick_group_type_conflict.message",
                            name,
                            otherTypes.joinToString(", ") { it.keyword },
                        ),
                    )
                }
            }
        }
}
