/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.util.findAnnotation
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.findParentOfType
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class RemoveAnnotationInspectionGadgetsFix(
    private val annotationName: String,
    private val name: String
) : InspectionGadgetsFix() {

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val decl = descriptor.psiElement.findParentOfType<PsiModifierListOwner>() ?: return
        decl.findAnnotation(annotationName)?.delete()
    }

    @Nls
    override fun getName() = name

    @Nls
    override fun getFamilyName() = name
}
