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

import com.demonwav.mcdev.platform.forge.sided.Side
import com.demonwav.mcdev.platform.forge.sided.SidedClassCache
import com.demonwav.mcdev.platform.forge.sided.SidedFieldCache
import com.demonwav.mcdev.platform.forge.sided.getInferenceReason
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField

class FieldDeclarationSideOnlyInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getDisplayName() = "Invalid usage of @SideOnly in field declaration"

    override fun getStaticDescription(): String? {
        return "A field in a class annotated for one side cannot be declared as being in the other side. For example, a class which is " +
            "annotated as @SideOnly(Side.SERVER) cannot contain a field which is annotated as @SideOnly(Side.CLIENT). Since a class that " +
            "is annotated with @SideOnly brings everything with it, @SideOnly annotated fields are usually useless in that case"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        private val project = holder.project
        private val fieldCache = SidedFieldCache.getInstance(project)
        private val classCache = SidedClassCache.getInstance(project)

        override fun visitField(field: PsiField) {
            if (!SideOnlyUtil.beginningCheck(field)) {
                return
            }

            val fieldState = fieldCache.getSideState(field) ?: return
            val containingClass = field.containingClass ?: return
            val containingClassState = classCache.getSideState(containingClass)

            if (fieldState.side === Side.INVALID) {
                return
            }

            val fieldInference = getInferenceReason(fieldState, field.name, project)

            if (containingClassState != null && containingClassState.side !== Side.INVALID) {
                if (fieldState.side !== containingClassState.side) {
                    val classInference = getInferenceReason(containingClassState, containingClass.shortName, project)

                    val text = buildString {
                        append("Field with side of ").append(fieldState.side.annotation)
                        append(" cannot be declared in a class with side of ").append(containingClassState.side.annotation)
                        fieldInference?.let { append('\n').append(it) }
                        classInference?.let { append('\n').append(it) }
                    }

                    holder.registerProblem(field, text, ProblemHighlightType.ERROR)
                }
            }

            (field.type as? PsiClassType)?.resolve()?.let { fieldClass ->
                val fieldClassState = classCache.getSideState(fieldClass) ?: return

                if (fieldClassState.side === Side.INVALID) {
                    return
                }

                if (fieldState.side !== fieldClassState.side) {
                    val fieldClassInference = getInferenceReason(fieldClassState, fieldClass.shortName, project)

                    val text = buildString {
                        append("Field with type side of ").append(fieldClassState.side.annotation)
                        append(" cannot be declared with side of ").append(fieldState.side.annotation)
                        fieldClassInference?.let { append('\n').append(it) }
                        fieldInference?.let { append('\n').append(it) }
                    }

                    holder.registerProblem(field, text, ProblemHighlightType.ERROR)
                }
            }
        }
    }
}
