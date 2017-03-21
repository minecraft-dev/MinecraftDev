/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.McpModule
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mixin.util.findFirstShadowTarget
import com.demonwav.mcdev.util.ActionData
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.gotoTargetElement
import com.demonwav.mcdev.util.invokeLater
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint

class GotoAtEntryAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon(e)

        if (data.element !is PsiIdentifier) {
            showBalloon(e)
            return
        }

        val mcpModule = data.instance.getModuleOfType(McpModuleType) ?: return showBalloon(e)

        mcpModule.srgManager?.srgMap?.done { srgMap ->
            var parent = data.element.parent

            if (parent is PsiMember) {
                val shadowTarget = parent.findFirstShadowTarget()
                if (shadowTarget != null) {
                    parent = shadowTarget
                }
            }

            if (parent is PsiField) {
                val reference = srgMap.findSrgField(parent) ?: return@done showBalloon(e)

                searchForText(mcpModule, e, data, reference.name)
            } else if (parent is PsiMethod) {
                val reference = srgMap.findSrgMethod(parent) ?: return@done showBalloon(e)

                searchForText(mcpModule, e, data, reference.name + reference.descriptor)
            } else {
                showBalloon(e)
            }
        } ?: showBalloon(e)
    }

    private fun searchForText(mcpModule: McpModule, e: AnActionEvent, data: ActionData, text: String) {
        for (virtualFile in mcpModule.accessTransformers) {
            val file = PsiManager.getInstance(data.project).findFile(virtualFile) ?: continue

            var found = false
            PsiSearchHelper.SERVICE.getInstance(data.project)
                .processElementsWithWord(
                    { element, _ ->
                        gotoTargetElement(element, data.editor, data.file)
                        found = true
                        false
                    },
                    LocalSearchScope(file),
                    text,
                    UsageSearchContext.ANY,
                    true
                )

            if (found) {
                return
            }
        }
        showBalloon(e)
    }

    private fun showBalloon(e: AnActionEvent) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("No access transformer entry found", null, LightColors.YELLOW, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon()

        val statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.dataContext))

        invokeLater { balloon.show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight) }
    }
}
