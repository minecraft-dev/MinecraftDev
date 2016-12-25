/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.internalNameAndDescriptor
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
        val method = element as? PsiMethod ?: return null
        val identifier = method.nameIdentifier ?: return null
        return if (method.modifierList.findAnnotation(MixinConstants.Annotations.OVERWRITE) != null) {
            OverwriteLineMarker(identifier, this)
        } else {
            null
        }
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<PsiElement>>) {
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val method = elt.parent as? PsiMethod ?: return
        val psiClass = getClassOfElement(method) ?: return

        val descriptor = method.internalNameAndDescriptor
        val targetMethods = MixinUtils.getAllMixedClasses(psiClass).values.stream()
                .flatMap { it.findMethodsByInternalNameAndDescriptor(descriptor) }
                .toTypedArray()
        PsiElementListNavigator.openTargets(e, targetMethods,
                "Choose target method of ${method.name}", null, MethodCellRenderer(false))
    }

}

class OverwriteLineMarker(identifier: PsiIdentifier, navHandler: GutterIconNavigationHandler<PsiIdentifier>)
    : MergeableLineMarkerInfo<PsiIdentifier>(identifier, identifier.textRange, AllIcons.Gutter.OverridingMethod,
        Pass.LINE_MARKERS, TOOLTIP_FUNCTION, navHandler, GutterIconRenderer.Alignment.LEFT) {

    override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is OverwriteLineMarker
    override fun getCommonTooltip(infos: MutableList<MergeableLineMarkerInfo<PsiElement>>) = TOOLTIP_FUNCTION
    override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<PsiElement>>) = ICON

    companion object {
        val ICON: Icon = AllIcons.Gutter.OverridingMethod
        val TOOLTIP_FUNCTION = FunctionUtil.constant<Any, String>("Go to target method")
    }

}
