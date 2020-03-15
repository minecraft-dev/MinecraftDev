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

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.util.FunctionUtil
import java.awt.event.MouseEvent

class MixinLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {

    companion object {
        private val ICON = MixinAssets.MIXIN_CLASS_ICON
        private val TOOLTIP_FUNCTION = FunctionUtil.constant<Any, String>("Go to target class")
    }

    override fun getName() = "Mixin line marker"
    override fun getIcon() = ICON

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiClass) {
            return null
        }

        val identifier = element.nameIdentifier ?: return null
        return if (element.isMixin) {
            LineMarker(identifier, this)
        } else {
            null
        }
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<PsiElement>>) {
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val psiClass = elt.parent as? PsiClass ?: return
        val targets = psiClass.mixinTargets
        if (targets.isNotEmpty()) {
            PsiElementListNavigator.openTargets(
                e, targets.toTypedArray(),
                "Choose target class of ${psiClass.name!!}", null, PsiClassListCellRenderer()
            )
        }
    }

    private class LineMarker(
        identifier: PsiIdentifier,
        navHandler: GutterIconNavigationHandler<PsiIdentifier>
    ) : MergeableLineMarkerInfo<PsiIdentifier>(
        identifier,
        identifier.textRange,
        MixinAssets.MIXIN_CLASS_ICON,
        TOOLTIP_FUNCTION,
        navHandler,
        GutterIconRenderer.Alignment.RIGHT
    ) {

        override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is LineMarker
        override fun getCommonTooltip(infos: List<MergeableLineMarkerInfo<PsiElement>>) = TOOLTIP_FUNCTION
        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<PsiElement>>) = MixinAssets.MIXIN_CLASS_ICON
    }
}
