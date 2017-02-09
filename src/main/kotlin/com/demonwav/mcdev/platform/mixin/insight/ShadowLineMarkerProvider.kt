/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.ide.util.MethodCellRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.util.FunctionUtil
import java.awt.event.MouseEvent
import javax.swing.Icon

class ShadowLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {

    override fun getName() = "@Shadow line marker"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiMember) {
            return null
        }

        val identifier = when (element) {
            is PsiMethod -> element.nameIdentifier
            is PsiField -> element.nameIdentifier
            else -> null
        } ?: return null

        return if (element.modifierList?.findAnnotation(MixinConstants.Annotations.SHADOW) != null) {
            LineMarker(identifier, this)
        } else {
            null
        }
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<PsiElement>>) {
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val member = elt.parent ?: return
        val targets = MixinUtils.getShadowedElement(member).targets
        if (targets.isEmpty()) {
            return
        }

        // Create correct renderer for element type
        val renderer = when (member) {
            is PsiMethod -> MethodCellRenderer(true)
            is PsiField -> DefaultPsiElementCellRenderer()
            else -> return
        }

        // TODO: Make this nicer when porting ShadowedMembers
        @Suppress("UNCHECKED_CAST")
        PsiElementListNavigator.openTargets(e, (targets as List<NavigatablePsiElement>).toTypedArray(),
                "Choose target class of ${(member as PsiMember).name!!}", null, renderer)
    }

    private class LineMarker(identifier: PsiIdentifier, navHandler: GutterIconNavigationHandler<PsiIdentifier>)
        : MergeableLineMarkerInfo<PsiIdentifier>(identifier, identifier.textRange, ICON,
            Pass.LINE_MARKERS, TOOLTIP_FUNCTION, navHandler, GutterIconRenderer.Alignment.LEFT) {

        override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is LineMarker
        override fun getCommonTooltip(infos: List<MergeableLineMarkerInfo<PsiElement>>) = TOOLTIP_FUNCTION
        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<PsiElement>>) = ICON

        private companion object {
            @JvmField val ICON: Icon = MixinAssets.SHADOW
            @JvmField val TOOLTIP_FUNCTION = FunctionUtil.constant<Any, String>("Go to Shadow element")
        }

    }


}
