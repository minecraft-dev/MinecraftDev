/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiModifierListOwner
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class RemoveAnnotationInspectionGadgetsFix(element: PsiModifierListOwner, private val name: String) :
    InspectionGadgetsFix() {

    private val pointer: SmartPsiPointer = SmartPsiPointer(element)

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val owner = pointer.element as? PsiModifierListOwner ?: return
        val list = owner.modifierList ?: return
        val annotation = list.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION) ?: return

        annotation.delete()
    }

    @Nls
    override fun getName() = name

    @Nls
    override fun getFamilyName(): String {
        return name
    }
}
