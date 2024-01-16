/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.findSourceClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.getPsiElementPopup
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.ui.awt.RelativePoint
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
            { "mixin target class indicator" },
        )
    }

    override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
        val psiClass = elt.parent as? PsiClass ?: return
        val name = psiClass.name ?: return
        val targets = psiClass.mixinTargets
            .mapNotNull { it.findSourceClass(psiClass.project, psiClass.resolveScope, canDecompile = true) }

        val singleTarget = targets.singleOrNull()
        if (singleTarget != null) {
            if (singleTarget.canNavigate()) {
                singleTarget.navigate(true)
            }
        } else if (targets.isNotEmpty()) {
            getPsiElementPopup(targets.toTypedArray<PsiElement>(), "Choose target class of $name")
                .show(RelativePoint(e))
        }
    }
}
