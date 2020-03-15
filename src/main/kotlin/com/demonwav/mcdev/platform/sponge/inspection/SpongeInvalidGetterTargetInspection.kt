/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

class SpongeInvalidGetterTargetInspection : BaseInspection() {

    override fun getDisplayName() = "@Getter targeted method does not exist"

    override fun buildErrorString(vararg infos: Any?) = staticDescription

    override fun getStaticDescription() =
        "@Getter must target a method accessible from the event class of this listener"

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitMethod(method: PsiMethod) {
                if (method.parameters.size < 2 || !method.isValidSpongeListener()) {
                    return
                }

                for (i in 1 until method.parameters.size) {
                    val parameter = method.parameters[i]
                    val getterAnnotation =
                        parameter.getAnnotation(SpongeConstants.GETTER_ANNOTATION) as? PsiAnnotation ?: continue

                    val getterTarget = getterAnnotation.resolveSpongeGetterTarget()
                    if (getterTarget == null) {
                        val attribute = getterAnnotation.findAttributeValue("value")
                        if (attribute == null) {
                            registerError(getterAnnotation, getterAnnotation)
                        } else {
                            registerError(attribute)
                        }
                        continue
                    }
                }
            }
        }
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
        if (infos.isEmpty()) {
            return null
        }

        val annotation = infos[0] as PsiAnnotation
        if (!annotation.isWritable) {
            return null
        }

        return QuickFix(annotation, "Add target method")
    }

    class QuickFix(annotation: PsiAnnotation, private val name: String) : InspectionGadgetsFix() {

        private val pointer = SmartPsiPointer(annotation)

        override fun doFix(project: Project, descriptor: ProblemDescriptor?) {
            doFix(project, pointer.element as PsiAnnotation)
        }

        override fun getFamilyName() = name

        override fun getName() = name

        companion object {

            fun doFix(project: Project, annotation: PsiAnnotation) {
                val value = JavaPsiFacade.getElementFactory(project).createExpressionFromText("\"\"", annotation)
                val newValue = annotation.setDeclaredAttributeValue(null, value)
                val editor = PsiEditorUtil.Service.getInstance().findEditorByPsiElement(annotation) ?: return
                editor.caretModel.removeSecondaryCarets()
                editor.selectionModel.removeSelection()
                editor.caretModel.moveToOffset(newValue.textOffset + 1)
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
            }
        }
    }
}
