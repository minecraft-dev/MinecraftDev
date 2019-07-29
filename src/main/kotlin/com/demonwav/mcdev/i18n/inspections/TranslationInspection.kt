/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.inspections

import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

abstract class TranslationInspection : AbstractBaseJavaLocalInspectionTool() {
    protected abstract fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (McpModuleType.isInModule(holder.file)) {
            return buildVisitor(holder)
        }

        return PsiElementVisitor.EMPTY_VISITOR
    }

    final override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (McpModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            listOf()
        }
    }
}
