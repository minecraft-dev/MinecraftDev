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

package com.demonwav.mcdev.platform.mixin.insight.target

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent

class MixinAccessorTargetLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {
    override fun getName() = MCDevBundle("mixin.accessor_target_line_marker.name")
    override fun getIcon() = MixinAssets.MIXIN_TARGET_ACCESSOR_MIXIN_ICON

    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        for (element in elements) {
            if (element !is PsiClass) {
                continue
            }

            val identifier = element.nameIdentifier ?: continue

            val mixins = FindMixinsAction.Util.findMixins(element, element.project) ?: continue
            if (mixins.none { it.isAccessorMixin }) {
                continue
            }

            result += LineMarkerInfo(
                identifier,
                identifier.textRange,
                icon,
                { MCDevBundle("mixin.accessor_target_line_marker.tooltip") },
                this,
                GutterIconRenderer.Alignment.LEFT,
                MCDevBundle.pointer("mixin.accessor_target_line_marker.accessible_name"),
            )
        }
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val targetClass = elt.parent as? PsiClass ?: return
        FindMixinsAction.Util.openFindMixinsUI(
            targetClass.project,
            targetClass,
            { show(RelativePoint(e)) }
        ) {
            it.isAccessorMixin
        }
    }
}
