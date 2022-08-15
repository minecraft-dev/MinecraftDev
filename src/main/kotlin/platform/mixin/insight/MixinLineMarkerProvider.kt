/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.findSourceClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import java.awt.event.MouseEvent

class MixinLineMarkerProvider : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiIdentifier> {

    override fun getName() = "Mixin line marker"
    override fun getIcon() = MixinAssets.MIXIN_CLASS_ICON

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiClass) {
            return null
        }

        val identifier = element.nameIdentifier ?: return null
        if (!element.isMixin) {
            return null
        }

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            icon,
            { "Go to target class" },
            this,
            GutterIconRenderer.Alignment.LEFT,
            { "mixin target class indicator" }
        )
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val psiClass = elt.parent as? PsiClass ?: return
        val name = psiClass.name ?: return
        val targets = psiClass.mixinTargets
            .mapNotNull { it.findSourceClass(psiClass.project, psiClass.resolveScope, canDecompile = true) }
        if (targets.isNotEmpty()) {
            PsiElementListNavigator.openTargets(
                e,
                targets.toTypedArray(),
                "Choose target class of $name",
                null,
                PsiClassListCellRenderer()
            )
        }
    }
}
