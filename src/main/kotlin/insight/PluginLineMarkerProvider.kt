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

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.util.FunctionUtil
import com.intellij.util.ThreeState
import com.intellij.util.ui.accessibility.ScreenReader

class PluginLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName() = MCDevBundle("insight.plugin.marker")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!element.isValid) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null

        val instance = MinecraftFacet.getInstance(module) ?: return null

        if (!instance.shouldShowPluginIcon(element)) {
            return null
        }

        val a11yText = if (!ScreenReader.isActive()) {
            // if screenreader isn't active, don't need to spend the extra time building the string
            val name = MCDevBundle("insight.plugin.marker.accessible_name_plugin")
            MCDevBundle("insight.plugin.marker.accessible_name", name)
        } else {
            val isMod = instance.modules.asSequence()
                .filter { it.shouldShowPluginIcon(element) }
                .map {
                    when (it.type) {
                        PlatformType.BUKKIT -> ThreeState.NO
                        PlatformType.SPIGOT -> ThreeState.NO
                        PlatformType.PAPER -> ThreeState.NO
                        PlatformType.ARCHITECTURY -> ThreeState.YES
                        PlatformType.FORGE -> ThreeState.YES
                        PlatformType.FABRIC -> ThreeState.YES
                        PlatformType.SPONGE -> ThreeState.NO
                        PlatformType.BUNGEECORD -> ThreeState.NO
                        PlatformType.WATERFALL -> ThreeState.NO
                        PlatformType.VELOCITY -> ThreeState.NO
                        PlatformType.MIXIN -> ThreeState.YES
                        PlatformType.MCP -> ThreeState.YES
                        PlatformType.ADVENTURE -> ThreeState.UNSURE
                    }
                }
                .filterNot { it == ThreeState.UNSURE }
                .distinct()
                .let { ThreeState.mostPositive(it.toList()) }

            val name = when (isMod) {
                ThreeState.YES -> MCDevBundle("insight.plugin.marker.accessible_name_mod")
                ThreeState.NO -> MCDevBundle("insight.plugin.marker.accessible_name_plugin")
                ThreeState.UNSURE -> MCDevBundle("insight.plugin.marker.accessible_name_unsure")
            }

            MCDevBundle("insight.plugin.marker.accessible_name", name)
        }

        @Suppress("MoveLambdaOutsideParentheses")
        return LineMarkerInfo(
            element,
            element.textRange,
            GeneralAssets.PLUGIN,
            FunctionUtil.nullConstant(),
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { a11yText },
        )
    }
}
