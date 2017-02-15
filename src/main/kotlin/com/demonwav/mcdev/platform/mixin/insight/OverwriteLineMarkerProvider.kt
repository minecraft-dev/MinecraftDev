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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.memberReference
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeHighlighting.Pass
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
import javax.swing.Icon

class OverwriteLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {

    override fun getName() = "Mixin @Overwrite line marker"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiMethod) {
            return null
        }

        val identifier = element.nameIdentifier ?: return null
        return if (element.modifierList.findAnnotation(MixinConstants.Annotations.OVERWRITE) != null) {
            LineMarker(identifier, this)
        } else {
            null
        }
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<PsiElement>>) {
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val method = elt.parent as? PsiMethod ?: return
        val psiClass = method.containingClass ?: return

        // TODO: Implement without member reference
        val reference = method.memberReference
        val targetMethods = MixinUtils.getAllMixedClasses(psiClass).values.stream()
                .flatMap { it.findMethods(reference) }
                .toTypedArray()
        if (targetMethods.isNotEmpty()) {
            PsiElementListNavigator.openTargets(e, targetMethods,
                    "Choose target method of ${method.name}", null, MethodCellRenderer(false))
        }
    }

    private class LineMarker(identifier: PsiIdentifier, navHandler: GutterIconNavigationHandler<PsiIdentifier>)
        : MergeableLineMarkerInfo<PsiIdentifier>(identifier, identifier.textRange, ICON,
            Pass.LINE_MARKERS, TOOLTIP_FUNCTION, navHandler, GutterIconRenderer.Alignment.LEFT) {

        override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is LineMarker
        override fun getCommonTooltip(infos: List<MergeableLineMarkerInfo<PsiElement>>) = TOOLTIP_FUNCTION
        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<PsiElement>>) = ICON

        private companion object {
            @JvmField val ICON: Icon = AllIcons.Gutter.OverridingMethod
            @JvmField val TOOLTIP_FUNCTION = FunctionUtil.constant<Any, String>("Go to target method")
        }

    }


}

