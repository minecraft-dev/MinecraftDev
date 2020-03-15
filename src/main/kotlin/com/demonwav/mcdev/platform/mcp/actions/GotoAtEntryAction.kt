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

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.demonwav.mcdev.platform.mixin.util.findFirstShadowTarget
import com.demonwav.mcdev.util.ActionData
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.gotoTargetElement
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
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

        val srgManager = data.instance.getModuleOfType(McpModuleType)?.srgManager
        // Not all ATs are in MCP modules, fallback to this if possible
        // TODO try to find SRG references for all modules if current module isn't found?
            ?: SrgManager.findAnyInstance(data.project) ?: return showBalloon(e)

        srgManager.srgMap.onSuccess { srgMap ->
            var parent = data.element.parent

            if (parent is PsiMember) {
                val shadowTarget = parent.findFirstShadowTarget()
                if (shadowTarget != null) {
                    parent = shadowTarget
                }
            }

            when (parent) {
                is PsiField -> {
                    val reference = srgMap.getSrgField(parent) ?: parent.simpleQualifiedMemberReference
                    ?: return@onSuccess showBalloon(e)
                    searchForText(e, data, reference.name)
                }
                is PsiMethod -> {
                    val reference =
                        srgMap.getSrgMethod(parent) ?: parent.qualifiedMemberReference ?: return@onSuccess showBalloon(
                            e
                        )
                    searchForText(e, data, reference.name + reference.descriptor)
                }
                else ->
                    showBalloon(e)
            }
        }
    }

    private fun searchForText(e: AnActionEvent, data: ActionData, text: String) {
        val manager = ModuleManager.getInstance(data.project)
        manager.modules.asSequence()
            .mapNotNull { MinecraftFacet.getInstance(it, McpModuleType) }
            .flatMap { it.accessTransformers.asSequence() }
            .forEach { virtualFile ->
                val file = PsiManager.getInstance(data.project).findFile(virtualFile) ?: return@forEach

                var found = false
                PsiSearchHelper.getInstance(data.project)
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

        val project = e.project ?: return
        val statusBar = WindowManager.getInstance().getStatusBar(project)

        invokeLater { balloon.show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight) }
    }
}
