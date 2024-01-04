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
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLocalVariable
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class LocalVariableDeclarationSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Invalid usage of local variable declaration annotated with @SideOnly"

    override fun buildErrorString(vararg infos: Any): String {
        val error = infos[0] as Error
        return error.getErrorString(*SideOnlyUtil.getSubArray(infos))
    }

    override fun getStaticDescription() =
        "A variable whose class declaration is annotated with @SideOnly for one side cannot be declared in a class" +
            " or method that does not match the same side."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val annotation = infos[3] as PsiAnnotation

        return if (annotation.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(
                annotation.qualifiedName ?: return null,
                "Remove @SideOnly annotation from variable class declaration",
            )
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitLocalVariable(variable: PsiLocalVariable) {
                val psiClass = variable.findContainingClass() ?: return

                if (!SideOnlyUtil.beginningCheck(variable)) {
                    return
                }

                val type = variable.type as? PsiClassType ?: return

                val variableClass = type.resolve() ?: return

                val (variableAnnotation, variableSide) = SideOnlyUtil.getSideForClass(variableClass)
                if (variableAnnotation == null || variableSide === Side.NONE || variableSide === Side.INVALID) {
                    return
                }

                val (containingClassAnnotation, containingClassSide) = SideOnlyUtil.getSideForClass(psiClass)
                val (methodAnnotation, methodSide) = SideOnlyUtil.checkElementInMethod(variable)

                var classAnnotated = false

                if (containingClassAnnotation != null &&
                    containingClassSide !== Side.NONE && containingClassSide !== Side.INVALID
                ) {
                    if (variableSide !== containingClassSide) {
                        registerVariableError(
                            variable,
                            Error.VAR_CROSS_ANNOTATED_CLASS,
                            variableAnnotation.renderSide(variableSide),
                            containingClassAnnotation.renderSide(containingClassSide),
                            variableClass.getAnnotation(variableAnnotation.annotationName),
                        )
                    }
                    classAnnotated = true
                }

                if (methodAnnotation == null || methodSide === Side.INVALID) {
                    return
                }

                if (variableSide !== methodSide) {
                    if (methodSide === Side.NONE) {
                        if (!classAnnotated) {
                            registerVariableError(
                                variable,
                                Error.VAR_UNANNOTATED_METHOD,
                                variableAnnotation.renderSide(variableSide),
                                methodAnnotation.renderSide(methodSide),
                                variableClass.getAnnotation(variableAnnotation.annotationName),
                            )
                        }
                    } else {
                        registerVariableError(
                            variable,
                            Error.VAR_CROSS_ANNOTATED_METHOD,
                            variableAnnotation.renderSide(variableSide),
                            methodAnnotation.renderSide(methodSide),
                            variableClass.getAnnotation(variableAnnotation.annotationName),
                        )
                    }
                }
            }
        }
    }

    enum class Error {
        VAR_CROSS_ANNOTATED_CLASS {
            override fun getErrorString(vararg infos: Any): String {
                return "A local variable whose class is annotated with ${infos[0]} " +
                    "cannot be used in a class annotated with ${infos[1]}"
            }
        },
        VAR_CROSS_ANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "A local variable whose class is annotated with ${infos[0]} " +
                    "cannot be used in a method annotated with ${infos[1]}"
            }
        },
        VAR_UNANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "A local variable whose class is annotated with ${infos[0]} " +
                    "cannot be used in an un-annotated method."
            }
        }, ;

        abstract fun getErrorString(vararg infos: Any): String
    }
}
