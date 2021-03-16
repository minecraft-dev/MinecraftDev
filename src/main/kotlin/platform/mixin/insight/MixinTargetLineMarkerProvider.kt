/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.findAccessorTarget
import com.demonwav.mcdev.platform.mixin.util.findFirstOverwriteTarget
import com.demonwav.mcdev.platform.mixin.util.findFirstShadowTarget
import com.demonwav.mcdev.platform.mixin.util.findInvokerTarget
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import com.intellij.util.Function
import com.intellij.util.PsiNavigateUtil
import java.awt.event.MouseEvent

class MixinTargetLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName() = "Mixin target line marker"
    override fun getIcon() = MixinAssets.SHADOW

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiMember) {
            return null
        }

        val identifier = when (element) {
            is PsiMethod -> element.nameIdentifier
            is PsiField -> element.nameIdentifier
            else -> null
        } ?: return null

        // Check if this is a Mixin target element
        // TODO add more target types, @Inject, etc
        val targetInfo = element.findShadow()
            ?: (element as? PsiMethod)?.findOverwrite()
            ?: element.findInvoker()
            ?: element.findAccessor()
            ?: return null

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            icon,
            Function { "Go to the ${targetInfo.text} target" },
            AccessorGutterIconNavigationHandler(identifier.createSmartPointer(), targetInfo.target),
            GutterIconRenderer.Alignment.LEFT
        )
    }

    private class TargetInfo(val target: SmartPsiElementPointer<out PsiMember>, val text: String)

    private fun PsiMember.findShadow(): TargetInfo? = this.findFirstShadowTarget()?.let { TargetInfo(it, "@Shadow") }
    private fun PsiMember.findAccessor(): TargetInfo? = this.findAccessorTarget()?.let { TargetInfo(it, "@Accessor") }
    private fun PsiMember.findInvoker(): TargetInfo? = this.findInvokerTarget()?.let { TargetInfo(it, "@Invoker") }
    private fun PsiMethod.findOverwrite(): TargetInfo? =
        this.findFirstOverwriteTarget()?.let { TargetInfo(it, "@Overwrite") }

    private class AccessorGutterIconNavigationHandler(
        private val identifierPointer: SmartPsiElementPointer<PsiIdentifier>,
        private val targetPointer: SmartPsiElementPointer<out PsiMember>
    ) : GutterIconNavigationHandler<PsiIdentifier> {
        override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
            val element = identifierPointer.element ?: return
            if (element != elt) {
                return
            }

            val target = targetPointer.element ?: return
            PsiNavigateUtil.navigate(target)
        }
    }
}
