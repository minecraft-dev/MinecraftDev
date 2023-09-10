/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFunctionalExpression
import com.intellij.psi.util.PsiExpressionTrimRenderer
import com.intellij.util.Function
import javax.swing.Icon
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

/**
 * A [LineMarkerProviderDescriptor] that will provide a line marker info icon
 * in the gutter for annotated event listeners. This is intended to be written to be
 * platform independent of which Minecraft Platform API is being used.
 */
class ListenerLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!MinecraftSettings.instance.isShowEventListenerGutterIcons) {
            return null
        }

        runCatchingKtIdeaExceptions {
            val identifier = element.toUElementOfType<UIdentifier>() ?: return null
            if (identifier.uastParent !is UMethod || identifier.uastEventListener == null) {
                return null
            }
        }

        // By this point, we can guarantee that the action of "go to declaration" will work
        // since the PsiClass can be resolved, meaning the event listener is listening to
        // a valid event.
        return EventLineMarkerInfo(
            element,
            element.textRange,
            icon,
            createHandler(),
        )
    }

    // This is a navigation handler that just simply goes and opens up the event's declaration,
    // even if the event target is a nested class.
    private fun createHandler(): GutterIconNavigationHandler<PsiElement> {
        return GutterIconNavigationHandler handler@{ _, element ->
            val (eventClass, _) = element.toUElementOfType<UIdentifier>()?.uastEventListener ?: return@handler
            FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration")
            if (eventClass.canNavigate()) {
                eventClass.navigate(true)
            }
        }
    }

    override fun getName() = MCDevBundle("insight.event_listener.marker")
    override fun getIcon() = GeneralAssets.LISTENER

    private class EventLineMarkerInfo(
        element: PsiElement,
        range: TextRange,
        icon: Icon,
        handler: GutterIconNavigationHandler<PsiElement>,
    ) : MergeableLineMarkerInfo<PsiElement>(
        element,
        range,
        icon,
        Function { MCDevBundle("insight.event_listener.marker.goto") },
        handler,
        GutterIconRenderer.Alignment.RIGHT,
        { MCDevBundle("insight.event_listener.marker.accessible_name") },
    ) {

        override fun canMergeWith(info: MergeableLineMarkerInfo<*>): Boolean {
            if (info !is EventLineMarkerInfo) {
                return false
            }

            val otherElement = info.getElement()
            val myElement = element
            return otherElement != null && myElement != null
        }

        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<*>>) = myIcon!!

        override fun getCommonTooltip(infos: List<MergeableLineMarkerInfo<*>>): Function<in PsiElement, String> =
            Function { MCDevBundle("insight.event_listener.marker.multiple") }

        override fun getElementPresentation(element: PsiElement): String {
            val parent = element.parent
            if (parent is PsiFunctionalExpression) {
                return PsiExpressionTrimRenderer.render(parent as PsiExpression)
            }
            return super.getElementPresentation(element)
        }
    }
}
