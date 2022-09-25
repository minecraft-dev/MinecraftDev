/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.refactoring.suggested.createSmartPointer
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class RemoveAnnotationInspectionGadgetsFix(element: PsiAnnotation, private val name: String) : InspectionGadgetsFix() {

    private val pointer: SmartPsiElementPointer<PsiAnnotation> = element.createSmartPointer()

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        pointer.element?.delete()
    }

    @Nls
    override fun getName() = name

    @Nls
    override fun getFamilyName() = name
}
