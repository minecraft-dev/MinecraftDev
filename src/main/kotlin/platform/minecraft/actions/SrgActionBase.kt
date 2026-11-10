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

package com.demonwav.mcdev.platform.minecraft.actions

import com.demonwav.mcdev.platform.minecraft.MinecraftModuleType
import com.demonwav.mcdev.platform.minecraft.mappings.Mappings
import com.demonwav.mcdev.platform.mixin.handlers.ShadowHandler
import com.demonwav.mcdev.util.ActionData
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.showBalloon
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference

abstract class SrgActionBase : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon(e, "Unknown failure")

        if (data.element !is PsiIdentifier) {
            showBalloon(e, "Not a valid element")
            return
        }

        val minecraftModule = data.instance.getModuleOfType(MinecraftModuleType)

        val mappingsManager = minecraftModule?.mappingsManager ?: return performWithMappings(e, data, data.element, null)
        mappingsManager.mappings.onSuccess { srgMap ->
            performWithMappings(e, data, data.element, srgMap)
        }.onError {
            showBalloon(e, it.message ?: "No MCP data available")
        }
    }

    private fun performWithMappings(
        e: AnActionEvent,
        data: ActionData,
        element: PsiIdentifier,
        srgMap: Mappings?,
    ) {
        var parent = element.parent ?: return showBalloon(e, "Not a valid element")

        if (parent is PsiMember) {
            val shadowTarget = ShadowHandler.Util.getInstance()?.findFirstShadowTargetForReference(parent)?.element
            if (shadowTarget != null) {
                parent = shadowTarget
            }
        }

        if (parent is PsiReference) {
            parent = parent.resolve() ?: return showBalloon(e, "Not a valid element")
        }

        withSrgTarget(parent, srgMap, e, data)
    }

    abstract fun withSrgTarget(parent: PsiElement, srgMap: Mappings?, e: AnActionEvent, data: ActionData)
}
