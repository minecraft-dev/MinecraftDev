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
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.PsiNavigateUtil
import java.awt.event.MouseEvent

class MixinTargetLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName() = "Mixin target line marker"
    override fun getIcon() = MixinAssets.SHADOW

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiIdentifier>? {
        if (element !is PsiMember) {
            return null
        }
        val containingClass = element.containingClass ?: return null
        if (!containingClass.isMixin) {
            return null
        }

        val identifier = when (element) {
            is PsiMethod -> element.nameIdentifier
            is PsiField -> element.nameIdentifier
            else -> null
        } ?: return null

        val (handler, annotation) = element.annotations.mapFirstNotNull { annotation ->
            annotation.qualifiedName?.let { qName ->
                MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project)?.let { it to annotation }
            }
        } ?: return null
        if (handler.isUnresolved(annotation) != null) {
            return null
        }
        val simpleName = annotation.qualifiedName?.substringAfterLast('.') ?: return null

        return LineMarkerInfo(
            identifier,
            identifier.textRange,
            icon,
            { "Go to the $simpleName target" },
            MixinGutterIconNavigationHandler(identifier.createSmartPointer(), annotation.createSmartPointer(), handler),
            GutterIconRenderer.Alignment.LEFT,
            { "mixin $simpleName target indicator" }
        )
    }

    private class MixinGutterIconNavigationHandler(
        private val identifierPointer: SmartPsiElementPointer<PsiIdentifier>,
        private val annotationPointer: SmartPsiElementPointer<PsiAnnotation>,
        private val handler: MixinAnnotationHandler
    ) : GutterIconNavigationHandler<PsiIdentifier> {
        override fun navigate(e: MouseEvent, elt: PsiIdentifier) {
            val element = identifierPointer.element ?: return
            if (element != elt) {
                return
            }
            val annotation = annotationPointer.element ?: return
            val targets = handler.resolveForNavigation(annotation)
            val editor = FileEditorManager.getInstance(elt.project).selectedTextEditor
            when (targets.size) {
                0 -> {
                    if (editor != null) {
                        HintManager.getInstance().showErrorHint(
                            editor,
                            "Cannot find corresponding element in source code"
                        )
                    }
                }
                1 -> {
                    PsiNavigateUtil.navigate(targets[0])
                }
                else -> {
                    if (editor != null) {
                        NavigationUtil.getPsiElementPopup(targets.toTypedArray(), "Choose Target")
                            .showInBestPositionFor(editor)
                    } else {
                        NavigationUtil.getPsiElementPopup(targets.toTypedArray(), "Choose Target")
                            .show(RelativePoint(e))
                    }
                }
            }
        }
    }
}
