/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.inspections

import com.intellij.codeInspection.BaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod

abstract class TranslationInspection : BaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
            }

            override fun visitClass(aClass: PsiClass) {
            }

            override fun visitField(field: PsiField) {
            }

            override fun visitElement(element: PsiElement?) {
                addDescriptors(checkElement(element, holder.manager, isOnTheFly))
            }

            override fun visitFile(file: PsiFile) {
            }

            private fun addDescriptors(descriptors: Array<ProblemDescriptor>?) {
                if (descriptors != null) {
                    for (descriptor in descriptors) {
                        holder.registerProblem(descriptor)
                    }
                }
            }
        }
    }

    open fun checkElement(element: PsiElement?, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        return null
    }
}
