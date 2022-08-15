/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.inspection

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

class UnresolvedReferenceInspection : LocalInspectionTool() {

    override fun getStaticDescription() = "Reports unresolved references in Fabric mod JSON files."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (holder.file.name == FabricConstants.FABRIC_MOD_JSON) {
            return Visitor(holder)
        }
        return PsiElementVisitor.EMPTY_VISITOR
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        if (file.name == FabricConstants.FABRIC_MOD_JSON) {
            return super.processFile(file, manager)
        }
        return emptyList()
    }

    private class Visitor(private val holder: ProblemsHolder) : JsonElementVisitor() {

        override fun visitStringLiteral(literal: JsonStringLiteral) {
            for (reference in literal.references) {
                if (reference !is InspectionReference) {
                    continue
                }

                if (reference.unresolved) {
                    holder.registerProblem(
                        literal,
                        "Cannot resolve ${reference.description}".format(reference.canonicalText),
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                        reference.rangeInElement
                    )
                }
            }
        }
    }
}
