/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.util.Constants
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile

class SpongeLoggingInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun getStaticDescription() =
        "Sponge provides a ${Constants.SLF4J_LOGGER} logger implementation using @Inject in your plugin class. " +
            "You should not use ${Constants.JAVA_UTIL_LOGGER} in your plugin."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return if (SpongeModuleType.isInModule(holder.file)) {
            Visitor(holder)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (SpongeModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            emptyList()
        }
    }

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitField(field: PsiField) {
            val element = field.typeElement ?: return
            val name = (field.type as? PsiClassType)?.fullQualifiedName ?: return

            if (name != Constants.JAVA_UTIL_LOGGER) {
                return
            }

            holder.registerProblem(
                element,
                "Sponge plugins should use ${Constants.SLF4J_LOGGER} rather than ${Constants.JAVA_UTIL_LOGGER}.",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }
}
