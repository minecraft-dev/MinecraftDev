/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.inspections

import com.demonwav.mcdev.platform.mcp.aw.AwFile
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwEntryMixin
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwMemberNameMixin
import com.demonwav.mcdev.util.childOfType
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.plugins.groovy.codeInspection.fixes.RemoveElementQuickFix

class DuplicateAwEntryInspection : LocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is AwFile)
            return null
        val collected = HashMap<Pair<PsiElement, String>, MutableList<AwEntryMixin>>()
        file.entries.forEach {
            val target = it.childOfType<AwMemberNameMixin>()?.resolve()
            val accessKind = it.accessKind
            if (target != null && accessKind != null)
                (collected.getOrCreate(Pair(target, accessKind)) { ArrayList() }) += it
        }
        val problems = ArrayList<ProblemDescriptor>()
        collected.forEach { (sort, matches) ->
            if (sort.first is PsiNamedElement)
                if (matches.size > 1)
                    for (match in matches)
                        problems += manager.createProblemDescriptor(
                            match,
                            "Duplicate entry for \"${sort.second}  ${(sort.first as PsiNamedElement).name}\"",
                            RemoveElementQuickFix("Remove duplicate"),
                            ProblemHighlightType.WARNING,
                            isOnTheFly
                        )
        }
        return problems.toTypedArray()
    }

    override fun runForWholeFile(): Boolean {
        return true
    }

    override fun getDisplayName(): String {
        return "Duplicate AW entry"
    }

    override fun getStaticDescription(): String {
        return "Warns when the same element has its accessibility, mutability, " +
            "or extensibility changed multiple times in one file."
    }
}
