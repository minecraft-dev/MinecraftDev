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
        val variableClass = infos[3] as PsiClass

        return if (variableClass.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(
                variableClass,
                "Remove @SideOnly annotation from variable class declaration"
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

                val variableSide = SideOnlyUtil.getSideForClass(variableClass)
                if (variableSide === Side.NONE || variableSide === Side.INVALID) {
                    return
                }

                val containingClassSide = SideOnlyUtil.getSideForClass(psiClass)
                val methodSide = SideOnlyUtil.checkElementInMethod(variable)

                var classAnnotated = false

                if (containingClassSide !== Side.NONE && containingClassSide !== Side.INVALID) {
                    if (variableSide !== containingClassSide) {
                        registerVariableError(
                            variable,
                            Error.VAR_CROSS_ANNOTATED_CLASS,
                            variableSide.annotation,
                            containingClassSide.annotation,
                            variableClass
                        )
                    }
                    classAnnotated = true
                }

                if (methodSide === Side.INVALID) {
                    return
                }

                if (variableSide !== methodSide) {
                    if (methodSide === Side.NONE) {
                        if (!classAnnotated) {
                            registerVariableError(
                                variable,
                                Error.VAR_UNANNOTATED_METHOD,
                                variableSide.annotation,
                                methodSide.annotation,
                                variableClass
                            )
                        }
                    } else {
                        registerVariableError(
                            variable,
                            Error.VAR_CROSS_ANNOTATED_METHOD,
                            variableSide.annotation,
                            methodSide.annotation,
                            variableClass
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
        };

        abstract fun getErrorString(vararg infos: Any): String
    }
}
