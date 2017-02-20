/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

abstract class MixinInspection : BaseJavaBatchLocalInspectionTool() {

    protected abstract fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor

    override final fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        // TODO: Check if file is actually a Mixin file?
        if (MixinModuleType.isInModule(holder.file)) {
            return buildVisitor(holder)
        }

        return PsiElementVisitor.EMPTY_VISITOR
    }

    override final fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (MixinModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            listOf()
        }
    }
}
