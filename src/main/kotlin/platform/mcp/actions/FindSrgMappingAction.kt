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

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap
import com.demonwav.mcdev.util.ActionData
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class FindSrgMappingAction : SrgActionBase() {

    override fun withSrgTarget(parent: PsiElement, srgMap: McpSrgMap, e: AnActionEvent, data: ActionData) {
        when (parent) {
            is PsiField -> {
                val srg = srgMap.getSrgField(parent) ?: return showBalloon("No SRG name found", e)
                showSuccessBalloon(data.editor, data.element, "SRG name: " + srg.name)
            }
            is PsiMethod -> {
                val srg = srgMap.getSrgMethod(parent) ?: return showBalloon("No SRG name found", e)
                showSuccessBalloon(data.editor, data.element, "SRG name: " + srg.name + srg.descriptor)
            }
            is PsiClass -> {
                val classMcpToSrg = srgMap.getSrgClass(parent) ?: return showBalloon("No SRG name found", e)
                showSuccessBalloon(data.editor, data.element, "SRG name: $classMcpToSrg")
            }
            else -> showBalloon("Not a valid element", e)
        }
    }
}
