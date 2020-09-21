/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents

class SideOnlyInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun getStaticDescription() = "SideOnly problems"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return Visitor(holder)
    }

    private class Visitor(private val problems: ProblemsHolder) : JavaElementVisitor() {

        // CHECK REFERENCES TO HARD AND SOFT SIDEONLY MEMBERS

        override fun visitClass(clazz: PsiClass) {
            val classSide = SideOnlyUtil.getContextSide(clazz, SideHardness.EITHER)

            val superClass = clazz.superClass
            if (superClass != null) {
                val targetSide = SideOnlyUtil.getContextSide(superClass, SideHardness.EITHER)
                val problemElement = clazz.extendsList?.referenceElements?.firstOrNull()
                if (problemElement != null && targetSide != null && targetSide.side != classSide?.side) {
                    problems.registerProblem(
                        problemElement,
                        SideOnlyUtil.createInspectionMessage(classSide, targetSide)
                    )
                }
            }

            val interfaceList = if (clazz.isInterface) {
                clazz.extendsList
            } else {
                clazz.implementsList
            }?.let { it.referencedTypes zip it.referenceElements } ?: emptyList()
            val sidedInterfaces = SideOnlyUtil.getSidedInterfaces(clazz)
            for ((itf, problemElement) in interfaceList) {
                val itfClass = itf.resolve() ?: continue
                val targetSide = SideOnlyUtil.getContextSide(itfClass, SideHardness.EITHER)
                val sidedInterface = sidedInterfaces[itfClass.qualifiedName]
                if (targetSide != null && targetSide.side != classSide?.side && targetSide.side != sidedInterface) {
                    problems.registerProblem(
                        problemElement,
                        SideOnlyUtil.createInspectionMessage(classSide, targetSide)
                    )
                }
            }
        }

        override fun visitField(field: PsiField) {
            checkEitherAccess(field.type, field.typeElement)
        }

        override fun visitMethod(method: PsiMethod) {
            checkEitherAccess(method.returnType, method.returnTypeElement)
            for (parameter in method.parameterList.parameters) {
                checkEitherAccess(parameter.type, parameter.typeElement)
            }
            for ((type, element) in method.throwsList.referencedTypes zip method.throwsList.referenceElements) {
                checkEitherAccess(type, element)
            }

            val body = method.body ?: return
            SideOnlyUtil.analyzeBodyForSoftSideProblems(body, problems)
        }

        override fun visitLambdaExpression(expression: PsiLambdaExpression) {
            // TODO: lambda parameter types?

            val body = expression.body ?: return
            SideOnlyUtil.analyzeBodyForSoftSideProblems(body, problems)
        }

        private fun checkEitherAccess(targetType: PsiType?, element: PsiElement?) {
            targetType ?: return
            element ?: return

            val targetClass = SideOnlyUtil.getClassInType(targetType) ?: return
            val contextSide = SideOnlyUtil.getContextSide(element, SideHardness.EITHER)
            val targetSide = SideOnlyUtil.getContextSide(targetClass, SideHardness.EITHER)
            if (targetSide != null && targetSide.side != contextSide?.side) {
                problems.registerProblem(element, SideOnlyUtil.createInspectionMessage(contextSide, targetSide))
            }
        }

        // CHECK REFERENCES TO HARD SIDEONLY MEMBERS

        override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
            // class references in annotations are always legal
            if (expression.parentOfType(PsiAnnotation::class, PsiMember::class, PsiClass::class) is PsiAnnotation) {
                return
            }

            checkHardAccess(expression.operand.type, expression.operand)
        }

        override fun visitInstanceOfExpression(expression: PsiInstanceOfExpression) {
            checkHardAccess(expression.checkType?.type, expression.checkType)
        }

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            checkHardAccess(expression.resolveMethod(), expression.methodExpression.referenceNameElement)
        }

        override fun visitNewExpression(expression: PsiNewExpression) {
            checkHardAccess(
                expression.classOrAnonymousClassReference?.resolve(),
                expression.classOrAnonymousClassReference
            )
        }

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            val field = expression.resolve() as? PsiField ?: return
            checkHardAccess(field, expression.referenceNameElement)
        }

        override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
            checkHardAccess(expression.potentiallyApplicableMember, expression.referenceNameElement)
        }

        override fun visitTypeCastExpression(expression: PsiTypeCastExpression) {
            checkHardAccess(expression.castType?.type, expression.castType)
        }

        private fun checkHardAccess(target: PsiElement?, reference: PsiElement?) {
            target ?: return
            reference ?: return

            val targetSide = SideOnlyUtil.getContextSide(target, SideHardness.HARD) ?: return
            val contextSide = getContextSideForHardAccess(reference)
            if (targetSide.side != contextSide?.side) {
                val contextSideForMsg = SideOnlyUtil.getContextSide(reference, SideHardness.EITHER)
                problems.registerProblem(reference, SideOnlyUtil.createInspectionMessage(contextSideForMsg, targetSide))
            }
        }

        private fun checkHardAccess(targetType: PsiType?, element: PsiElement?) {
            targetType ?: return
            checkHardAccess(SideOnlyUtil.getClassInType(targetType), element)
        }

        private fun getContextSideForHardAccess(element: PsiElement): SideInstance? {
            // Same as SideOnlyUtil.getContextSide(element, SideHardness.EITHER), with the exception that soft-sidedness
            // of methods are ignored, as the mere presence of these methods can trip the verifier.
            val softSide = SideOnlyUtil.getContextSide(element, SideHardness.SOFT)
            val hardSide = SideOnlyUtil.getContextSide(element, SideHardness.HARD)
            return if (softSide != null &&
                softSide.element !is PsiMethod &&
                softSide.element.parents().contains(hardSide?.element)
            ) {
                softSide
            } else {
                hardSide
            }
        }
    }
}
