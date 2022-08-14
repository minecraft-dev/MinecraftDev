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
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class RemoveAnnotationInspectionGadgetsFix(element: PsiAnnotation, private val name: String) : InspectionGadgetsFix() {

    private val pointer: SmartPsiPointer = SmartPsiPointer(element)

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        (pointer.element as? PsiAnnotation)?.delete()
    }

    @Nls
    override fun getName() = name

    @Nls
    override fun getFamilyName() = name
}
