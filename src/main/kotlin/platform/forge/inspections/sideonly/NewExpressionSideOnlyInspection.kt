/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.util.findContainingClass
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNewExpression
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class NewExpressionSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Invalid usage of class annotated with @SideOnly"

    override fun buildErrorString(vararg infos: Any) =
        "A class annotated with @SideOnly can only be used in other matching annotated classes and methods"

    override fun getStaticDescription(): String {
        return "A class that is annotated as @SideOnly(Side.CLIENT) or @SideOnly(Side.SERVER) cannot be " +
            "used in classes or methods which are annotated differently, or not at all. Since the " +
            "irrelevant code is removed when operating as a server or a client, common code cannot " +
            "use @SideOnly annotated classes either."
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
        val annotation = infos[0] as? PsiAnnotation ?: return null

        return if (annotation.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(
                annotation.qualifiedName ?: return null,
                "Remove @SideOnly annotation from class declaration"
            )
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitNewExpression(expression: PsiNewExpression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return
                }

                val element = expression.classReference ?: return

                val psiElement = (element.resolve() ?: return) as? PsiClass ?: return

                var classAnnotation: SideAnnotation? = null
                var classSide = Side.NONE
                var offender: PsiClass? = null
                for ((hierarchyAnnotation, hierarchySide, clazz) in SideOnlyUtil.checkClassHierarchy(psiElement)) {
                    if (hierarchySide !== Side.NONE && hierarchySide !== Side.INVALID) {
                        classAnnotation = hierarchyAnnotation
                        classSide = hierarchySide
                        offender = clazz
                        break
                    }
                }

                @Suppress("KotlinConstantConditions") // kotlin is wrong
                if (classAnnotation == null || classSide == Side.NONE || offender == null) {
                    return
                }

                // Check the class(es) the element is in
                val containingClass = expression.findContainingClass() ?: return

                val (_, containingClassSide) = SideOnlyUtil.getSideForClass(containingClass)
                // Check the method the element is in
                val (_, methodSide) = SideOnlyUtil.checkElementInMethod(expression)

                var classAnnotated = false

                if (containingClassSide !== Side.NONE && containingClassSide !== Side.INVALID) {
                    if (containingClassSide !== classSide) {
                        registerError(expression, offender.getAnnotation(classAnnotation.annotationName))
                    }
                    classAnnotated = true
                } else {
                    if (methodSide === Side.INVALID) {
                        // It's not in a method
                        registerError(expression, offender.getAnnotation(classAnnotation.annotationName))
                        return
                    }
                }

                // Put error on for method
                if (classSide !== methodSide && methodSide !== Side.INVALID) {
                    if (methodSide === Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            registerError(expression, offender.getAnnotation(classAnnotation.annotationName))
                        }
                    } else {
                        registerError(expression, offender.getAnnotation(classAnnotation.annotationName))
                    }
                }
            }
        }
    }
}
