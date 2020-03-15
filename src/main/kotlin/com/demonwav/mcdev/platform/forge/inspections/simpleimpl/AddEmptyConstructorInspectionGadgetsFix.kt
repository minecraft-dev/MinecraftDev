/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.simpleimpl

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer
import com.siyeh.ig.InspectionGadgetsFix

class AddEmptyConstructorInspectionGadgetsFix(element: PsiClass, private val name: String) : InspectionGadgetsFix() {

    private val pointer: SmartPsiPointer = SmartPsiPointer(element)

    override fun doFix(project: Project, descriptor: ProblemDescriptor?) {
        val clazz = pointer.element as? PsiClass ?: return
        clazz.addBefore(JavaPsiFacade.getElementFactory(project).createConstructor(), clazz.methods[0])
    }

    override fun getName() = name

    override fun getFamilyName() = name
}
