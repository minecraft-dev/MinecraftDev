/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.inspections

import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.codeInspection.BaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

abstract class TranslationInspection : BaseJavaLocalInspectionTool() {
    protected abstract fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor

    override final fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (McpModuleType.isInModule(holder.file)) {
            return buildVisitor(holder)
        }

        return PsiElementVisitor.EMPTY_VISITOR
    }

    override final fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (McpModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            listOf()
        }
    }
}
