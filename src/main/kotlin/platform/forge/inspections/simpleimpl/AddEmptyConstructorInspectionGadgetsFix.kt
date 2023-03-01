/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.simpleimpl

import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.siyeh.ig.InspectionGadgetsFix

object AddEmptyConstructorInspectionGadgetsFix : InspectionGadgetsFix() {

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val clazz = descriptor.psiElement.findContainingClass() ?: return
        clazz.addBefore(JavaPsiFacade.getElementFactory(project).createConstructor(), clazz.methods[0])
    }

    override fun getName() = "Add empty constructor"

    override fun getFamilyName() = name
}
