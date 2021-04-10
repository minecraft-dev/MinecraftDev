/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.util.gotoTargetElement
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFunctionalExpression
import com.intellij.psi.util.PsiExpressionTrimRenderer
import com.intellij.psi.util.PsiUtilCore
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

        val listener = element.toUElementOfType<UIdentifier>()?.uastEventListener ?: return null
        // By this point, we can guarantee that the action of "go to declaration" will work
        // since the PsiClass can be resolved, meaning the event listener is listening to
        // a valid event.
        return EventLineMarkerInfo(
            element,
            element.textRange,
            icon,
            createHandler(listener.second)
        )
    }

    // This is a navigation handler that just simply goes and opens up the event's declaration,
    // even if the event target is a nested class.
    private fun createHandler(method: UMethod): GutterIconNavigationHandler<PsiElement> {
        return GutterIconNavigationHandler handler@{ _, element ->
            // We need to re-evaluate the targeted method, because if the method signature slightly changes before
            // IntelliJ decides to re-evaluate the method, but the class is no longer valid.
            // In this circumstance, we can find the class anyways because it's still a valid listener.
            val containingFile = element.containingFile ?: return@handler
            val parameter = method.uastEventParameterPair ?: return@handler

            val resolve = parameter.second.sourcePsi ?: return@handler
            val virtualFile = PsiUtilCore.getVirtualFile(resolve)

            if (virtualFile != null) {
                val project = element.project
                val editor = FileEditorManager.getInstance(project).selectedTextEditor

                if (editor != null) {
                    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration")
                    var navElement: PsiElement? = resolve.navigationElement
                    navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(resolve, navElement)
                    if (navElement != null) {
                        gotoTargetElement(navElement, editor, containingFile)
                    }
                }
            }
        }
    }

    override fun getName() = "Event Listener line marker"
    override fun getIcon() = GeneralAssets.LISTENER

    private class EventLineMarkerInfo constructor(
        element: PsiElement,
        range: TextRange,
        icon: Icon,
        handler: GutterIconNavigationHandler<PsiElement>
    ) : MergeableLineMarkerInfo<PsiElement>(
        element,
        range,
        icon,
        Function { "Go to Event declaration" },
        handler,
        GutterIconRenderer.Alignment.RIGHT,
        { "event listener indicator" }
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
            Function { "Multiple method overrides" }

        override fun getElementPresentation(element: PsiElement): String {
            val parent = element.parent
            if (parent is PsiFunctionalExpression) {
                return PsiExpressionTrimRenderer.render(parent as PsiExpression)
            }
            return super.getElementPresentation(element)
        }
    }
}
