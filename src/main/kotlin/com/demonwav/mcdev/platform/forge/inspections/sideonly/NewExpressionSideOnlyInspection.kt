/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.util.findContainingClass
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

    override fun getStaticDescription(): String? {
        return "A class that is annotated as @SideOnly(Side.CLIENT) or @SideOnly(Side.SERVER) cannot be " +
            "used in classes or methods which are annotated differently, or not at all. Since the " +
            "irrelevant code is removed when operating as a server or a client, common code cannot " +
            "use @SideOnly annotated classes either."
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val psiClass = infos[0] as PsiClass

        return if (psiClass.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(psiClass, "Remove @SideOnly annotation from class declaration")
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

                val list = SideOnlyUtil.checkClassHierarchy(psiElement)

                var classSide = Side.NONE

                var offender: PsiClass? = null
                for (pair in list) {
                    if (pair.first !== Side.NONE && pair.first !== Side.INVALID) {
                        classSide = pair.first
                        offender = pair.second
                        break
                    }
                }

                if (classSide === Side.NONE) {
                    return
                }

                // Check the class(es) the element is in
                val containingClass = expression.findContainingClass() ?: return

                val containingClassSide = SideOnlyUtil.getSideForClass(containingClass)
                // Check the method the element is in
                val methodSide = SideOnlyUtil.checkElementInMethod(expression)

                var classAnnotated = false

                if (containingClassSide !== Side.NONE && containingClassSide !== Side.INVALID) {
                    if (containingClassSide !== classSide) {
                        registerError(expression, offender)
                    }
                    classAnnotated = true
                } else {
                    if (methodSide === Side.INVALID) {
                        // It's not in a method
                        registerError(expression, offender)
                        return
                    }
                }

                // Put error on for method
                if (classSide !== methodSide && methodSide !== Side.INVALID) {
                    if (methodSide === Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            registerError(expression, offender)
                        }
                    } else {
                        registerError(expression, offender)
                    }
                }
            }
        }
    }
}
