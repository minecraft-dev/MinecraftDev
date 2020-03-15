/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

abstract class MixinInspection : AbstractBaseJavaLocalInspectionTool() {

    protected abstract fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        // TODO: Check if file is actually a Mixin file?
        if (MixinModuleType.isInModule(holder.file)) {
            return buildVisitor(holder)
        }

        return PsiElementVisitor.EMPTY_VISITOR
    }

    final override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (MixinModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            emptyList()
        }
    }
}
