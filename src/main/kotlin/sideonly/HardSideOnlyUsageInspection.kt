/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

class HardSideOnlyUsageInspection : BaseInspection() {
    override fun getDisplayName(): String {
        return "Usage of hard-SideOnly annotations"
    }

    override fun getStaticDescription(): String {
        return "Usage of hard-SideOnly annotations"
    }

    override fun buildErrorString(vararg infos: Any?): String {
        return "Usage of @${infos[0] as String}"
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return Visitor()
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix {
        return Fix(SmartPointerManager.createPointer(infos[1] as PsiAnnotation))
    }

    private class Visitor : BaseInspectionVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (SideOnlyUtil.getAnnotationSide(annotation, SideHardness.HARD) != Side.BOTH) {
                registerError(
                    annotation.navigationElement,
                    annotation.nameReferenceElement?.text ?: annotation.qualifiedName,
                    annotation
                )
            }
        }
    }

    private class Fix(private val annotation: SmartPsiElementPointer<PsiAnnotation>) : InspectionGadgetsFix() {
        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            val annotation = this.annotation.element ?: return
            val oldSide = SideOnlyUtil.getAnnotationSide(annotation, SideHardness.HARD)
            val newAnnotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText(
                "@${SideOnlyUtil.MCDEV_SIDEONLY_ANNOTATION}(${SideOnlyUtil.MCDEV_SIDE}.$oldSide)",
                annotation
            )
            val createdAnnotation = annotation.replace(newAnnotation)
            val codeStyleManager = JavaCodeStyleManager.getInstance(project)
            codeStyleManager.shortenClassReferences(createdAnnotation)
            createdAnnotation.containingFile?.let { codeStyleManager.optimizeImports(it) }
        }

        override fun getName() = "Replace with @CheckEnv"

        override fun getFamilyName() = name
    }
}
