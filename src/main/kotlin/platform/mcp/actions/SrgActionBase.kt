/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap
import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.demonwav.mcdev.platform.mixin.util.findFirstShadowTarget
import com.demonwav.mcdev.util.ActionData
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint

abstract class SrgActionBase : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon("Unknown failure", e)

        if (data.element !is PsiIdentifier) {
            showBalloon("Not a valid element", e)
            return
        }

        var parent = data.element.parent ?: return showBalloon("Not a valid element", e)

        if (parent is PsiMember) {
            val shadowTarget = parent.findFirstShadowTarget()?.element
            if (shadowTarget != null) {
                parent = shadowTarget
            }
        }

        if (parent is PsiReference) {
            parent = parent.resolve() ?: return showBalloon("Not a valid element", e)
        }

        val srgManager = data.instance.getModuleOfType(McpModuleType)?.srgManager
            // Not all ATs are in MCP modules, fallback to this if possible
            // TODO try to find SRG references for all modules if current module isn't found?
            ?: SrgManager.findAnyInstance(data.project)
        if (srgManager == null) {
            withSrgTarget(parent, null, e, data)
        } else {
            srgManager.srgMap.onSuccess { srgMap ->
                withSrgTarget(parent, srgMap, e, data)
            }.onError {
                showBalloon(it.message ?: "No MCP data available", e)
            }
        }
    }

    abstract fun withSrgTarget(parent: PsiElement, srgMap: McpSrgMap?, e: AnActionEvent, data: ActionData)

    protected fun getSrgClass(srgMap: McpSrgMap?, clazz: PsiClass): String? {
        return if (srgMap != null) {
            srgMap.getSrgClass(clazz)
        } else {
            clazz.fullQualifiedName
        }
    }

    protected fun getSrgMethod(srgMap: McpSrgMap?, element: PsiMethod): MemberReference? {
        return if (srgMap != null) {
            srgMap.getSrgMethod(element)
        } else {
            element.qualifiedMemberReference
        }
    }

    protected fun getSrgField(srgMap: McpSrgMap?, element: PsiField): MemberReference? {
        return if (srgMap != null) {
            srgMap.getSrgField(element)
        } else {
            element.qualifiedMemberReference
        }
    }

    protected fun showBalloon(message: String, e: AnActionEvent) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, LightColors.YELLOW, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon()

        val project = e.project ?: return
        val statusBar = WindowManager.getInstance().getStatusBar(project)

        invokeLater {
            balloon.show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight)
        }
    }

    protected fun showSuccessBalloon(editor: Editor, element: PsiElement, text: String) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(text, null, LightColors.SLIGHTLY_GREEN, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon()

        invokeLater {
            balloon.show(
                RelativePoint(
                    editor.contentComponent,
                    editor.visualPositionToXY(editor.offsetToVisualPosition(element.textRange.endOffset))
                ),
                Balloon.Position.atRight
            )
        }
    }
}
