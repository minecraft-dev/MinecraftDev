/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
                showSuccessBalloon(data.editor, data.element, "SRG name: " + classMcpToSrg)
            }
            else -> showBalloon("Not a valid element", e)
        }
    }
}
