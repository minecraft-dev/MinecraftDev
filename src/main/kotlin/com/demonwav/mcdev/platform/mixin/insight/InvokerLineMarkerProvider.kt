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
import com.demonwav.mcdev.platform.mixin.util.findInvokerTarget
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.ide.util.MethodCellRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.util.FunctionUtil
import java.awt.event.MouseEvent
import javax.swing.Icon

class InvokerLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {

    companion object {
        private val ICON: Icon = MixinAssets.SHADOW
        private val TOOLTIP_FUNCTION = FunctionUtil.constant<Any, String>("Go to Invoker element")
    }

    override fun getName() = "@Invoker line marker"
    override fun getIcon() = ICON

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiMember) {
            return null
        }

        val identifier = when (element) {
            is PsiMethod -> element.nameIdentifier
            is PsiField -> element.nameIdentifier
            else -> null
        } ?: return null

        // Check if @Invoker actually has a target
        element.findInvokerTarget() ?: return null
        return LineMarker(identifier, this)
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<PsiElement>>) {
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val member = elt.parent as? PsiMember ?: return
        val target = member.findInvokerTarget() ?: return

        // Create correct renderer for element type
        val renderer = when (member) {
            is PsiMethod -> MethodCellRenderer(true)
            is PsiField -> DefaultPsiElementCellRenderer()
            else -> return
        }

        PsiElementListNavigator.openTargets(
            e, arrayOf(target),
            "Choose target class of ${member.name!!}", null, renderer
        )
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
