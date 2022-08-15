/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.InheritanceUtil

class WrongEntityDataParameterClassInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun getStaticDescription() = "Reports when the class passed to an entity data parameter definition is " +
        "not the same as the containing entity class"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = Visitor(holder)

    class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
            super.visitMethodCallExpression(expression)
            if (expression == null) {
                return
            }

            val method = expression.resolveMethod() ?: return
            val className = method.containingClass?.fullQualifiedName ?: return
            val methodName = method.name

            if (className !in ENTITY_DATA_MANAGER_CLASSES || methodName !in DEFINE_ID_METHODS) return

            val containingClass = expression.findContainingClass() ?: return

            if (!isEntitySubclass(containingClass)) return

            val firstParameter = expression.argumentList.expressions.firstOrNull() ?: return
            val firstParameterGenericsClass =
                ((firstParameter.type as? PsiClassType)?.parameters?.firstOrNull() as? PsiClassType)?.resolve()
                    ?: return

            if (!containingClass.manager.areElementsEquivalent(containingClass, firstParameterGenericsClass)) {
                holder.registerProblem(
                    expression,
                    "Entity class does not match this entity class",
                    QuickFix(firstParameter)
                )
            }
        }
    }

    private class QuickFix(firstParameter: PsiExpression) : LocalQuickFixOnPsiElement(firstParameter) {
        override fun getText() = "Replace other entity class with this entity class"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val factory = JavaPsiFacade.getElementFactory(project)
            val firstParameter = startElement as? PsiExpression ?: return
            val containingClass = firstParameter.findContainingClass() ?: return

            firstParameter.replace(
                factory.createExpressionFromText(
                    "${containingClass.name}.class",
                    firstParameter
                )
            )
        }

        override fun getFamilyName() = name
    }

    companion object {
        private val ENTITY_CLASSES = setOf(
            "net.minecraft.entity.Entity",
            "net.minecraft.world.entity.Entity",
        )
        private val ENTITY_DATA_MANAGER_CLASSES = setOf(
            "net.minecraft.network.datasync.EntityDataManager",
            "net.minecraft.network.syncher.SynchedEntityData",
            "net.minecraft.entity.data.DataTracker"
        )
        private val DEFINE_ID_METHODS = setOf(
            "defineId",
            "createKey",
            "registerData"
        )

        private fun isEntitySubclass(clazz: PsiClass): Boolean =
            InheritanceUtil.getSuperClasses(clazz).any { it.qualifiedName in ENTITY_CLASSES }
    }
}
