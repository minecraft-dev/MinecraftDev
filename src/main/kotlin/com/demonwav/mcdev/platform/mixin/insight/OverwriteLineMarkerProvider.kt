/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.util.findFirstOverwriteTarget
import com.demonwav.mcdev.platform.mixin.util.findOverwriteTargets
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.icons.AllIcons
import com.intellij.ide.util.MethodCellRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.util.FunctionUtil
import java.awt.event.MouseEvent

class OverwriteLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {

    companion object {
        private val ICON = AllIcons.Gutter.OverridingMethod!!
        private val TOOLTIP_FUNCTION = FunctionUtil.constant<Any, String>("Go to target method")
    }

    override fun getName() = "Mixin @Overwrite line marker"
    override fun getIcon() = ICON

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiMethod) {
            return null
        }

        val identifier = element.nameIdentifier ?: return null

        // Check if @Overwrite actually has a target
        element.findFirstOverwriteTarget() ?: return null
        return LineMarker(identifier, this)
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<PsiElement>>) {
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val method = elt.parent as? PsiMethod ?: return
        val targets = method.findOverwriteTargets() ?: return
        if (targets.isNotEmpty()) {
            PsiElementListNavigator.openTargets(
                e, targets.toTypedArray(),
                "Choose target method of ${method.name}", null, MethodCellRenderer(false)
            )
        }
    }

    private class LineMarker(
        identifier: PsiIdentifier,
        navHandler: GutterIconNavigationHandler<PsiIdentifier>
    ) : MergeableLineMarkerInfo<PsiIdentifier>(
        identifier,
        identifier.textRange,
        ICON,
        TOOLTIP_FUNCTION,
        navHandler,
        GutterIconRenderer.Alignment.LEFT
    ) {

        override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is LineMarker
        override fun getCommonTooltip(infos: List<MergeableLineMarkerInfo<PsiElement>>) = TOOLTIP_FUNCTION
        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<PsiElement>>) = ICON
    }
}
