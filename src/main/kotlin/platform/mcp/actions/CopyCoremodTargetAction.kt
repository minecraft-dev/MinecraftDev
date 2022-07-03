/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap
import com.demonwav.mcdev.util.ActionData
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class CopyCoremodTargetAction : SrgActionBase() {
    override fun withSrgTarget(parent: PsiElement, srgMap: McpSrgMap, e: AnActionEvent, data: ActionData) {
        when (parent) {
            is PsiField -> {
                val containing = parent.containingClass ?: return showBalloon("No SRG name found", e)
                val classSrg = srgMap.getSrgClass(containing) ?: return showBalloon("No SRG name found", e)
                val srg = srgMap.getSrgField(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    Pair("target", "FIELD"),
                    Pair("class", classSrg),
                    Pair("fieldName", srg.name)
                )
            }
            is PsiMethod -> {
                val containing = parent.containingClass ?: return showBalloon("No SRG name found", e)
                val classSrg = srgMap.getSrgClass(containing) ?: return showBalloon("No SRG name found", e)
                val srg = srgMap.getSrgMethod(parent) ?: return showBalloon("No SRG name found", e)
                val srgDescriptor = srg.descriptor ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    Pair("target", "METHOD"),
                    Pair("class", classSrg),
                    Pair("methodName", srg.name),
                    Pair("methodDesc", srgDescriptor)
                )
            }
            is PsiClass -> {
                val classSrg = srgMap.getSrgClass(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    Pair("target", "CLASS"),
                    Pair("name", classSrg),
                )
            }
            else -> showBalloon("Not a valid element", e)
        }
    }

    private fun copyToClipboard(editor: Editor, element: PsiElement, vararg keys: Pair<String, String>) {
        val text = JsonObject(keys.toMap().mapValues { JsonPrimitive(it.value) }).toString()
        val stringSelection = StringSelection(text)
        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
        clpbrd.setContents(stringSelection, null)
        showSuccessBalloon(editor, element, "Copied Coremod Target Reference")
    }
}
