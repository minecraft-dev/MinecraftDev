/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiModifierListOwner
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

abstract class RemoveAnnotationInspectionGadgetsFix : InspectionGadgetsFix() {

    abstract val listOwner: PsiModifierListOwner?

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val owner = listOwner ?: return

        val list = owner.modifierList ?: return

        val annotation = list.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION) ?: return

        annotation.delete()
    }

    @Nls
    abstract override fun getName(): String

    @Nls
    override fun getFamilyName(): String {
        return name
    }
}
