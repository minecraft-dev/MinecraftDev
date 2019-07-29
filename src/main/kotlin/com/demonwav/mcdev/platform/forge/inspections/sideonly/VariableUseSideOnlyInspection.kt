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
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.source.PsiFieldImpl
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import org.jetbrains.annotations.Nls

class VariableUseSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Invalid usage of variable annotated with @SideOnly"

    override fun buildErrorString(vararg infos: Any): String {
        val error = infos[0] as Error
        return error.getErrorString(*SideOnlyUtil.getSubArray(infos))
    }

    override fun getStaticDescription() =
        "Variables which are declared with a @SideOnly annotation can only be used " +
            "in matching @SideOnly classes and methods."

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression?) {
                if (!SideOnlyUtil.beginningCheck(expression!!)) {
                    return
                }

                val declaration = expression.resolve() as? PsiFieldImpl ?: return

                var elementSide = SideOnlyUtil.checkField(declaration)

                // Check the class(es) the element is declared in
                val declarationContainingClass = declaration.containingClass ?: return

                val declarationClassHierarchySides = SideOnlyUtil.checkClassHierarchy(declarationContainingClass)

                val declarationClassSide = SideOnlyUtil.getFirstSide(declarationClassHierarchySides)

                // The element inherits the @SideOnly from it's parent class if it doesn't explicitly set it itself
                var inherited = false
                if (declarationClassSide !== Side.NONE && (elementSide === Side.INVALID || elementSide === Side.NONE)) {
                    inherited = true
                    elementSide = declarationClassSide
                }

                if (elementSide === Side.INVALID || elementSide === Side.NONE) {
                    return
                }

                // Check the class(es) the element is in
                val containingClass = expression.findContainingClass() ?: return

                val classSide = SideOnlyUtil.getSideForClass(containingClass)

                var classAnnotated = false

                if (classSide !== Side.NONE && classSide !== Side.INVALID) {
                    if (classSide !== elementSide) {
                        if (inherited) {
                            registerError(
                                expression.element,
                                Error.ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.annotation,
                                classSide.annotation,
                                declaration
                            )
                        } else {
                            registerError(
                                expression.element,
                                Error.ANNOTATED_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.annotation,
                                classSide.annotation,
                                declaration
                            )
                        }
                    }
                    classAnnotated = true
                }

                // Check the method the element is in
                val methodSide = SideOnlyUtil.checkElementInMethod(expression)

                // Put error on for method
                if (elementSide !== methodSide && methodSide !== Side.INVALID) {
                    if (methodSide === Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            if (inherited) {
                                registerError(
                                    expression.element,
                                    Error.ANNOTATED_CLASS_VAR_IN_UNANNOTATED_METHOD,
                                    elementSide.annotation,
                                    null,
                                    declaration
                                )
                            } else {
                                registerError(
                                    expression.element,
                                    Error.ANNOTATED_VAR_IN_UNANNOTATED_METHOD,
                                    elementSide.annotation,
                                    null,
                                    declaration
                                )
                            }
                        }
                    } else {
                        if (inherited) {
                            registerError(
                                expression.element,
                                Error.ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.annotation,
                                methodSide.annotation,
                                declaration
                            )
                        } else {
                            registerError(
                                expression.element,
                                Error.ANNOTATED_VAR_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.annotation,
                                methodSide.annotation,
                                declaration
                            )
                        }
                    }
                }
            }
        }
    }

    enum class Error {
        ANNOTATED_VAR_IN_UNANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Variable annotated with ${infos[0]} cannot be referenced in an un-annotated method."
            }
        },
        ANNOTATED_CLASS_VAR_IN_UNANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Variable declared in a class annotated with ${infos[0]} " +
                    "cannot be referenced in an un-annotated method."
            }
        },
        ANNOTATED_VAR_IN_CROSS_ANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Variable annotated with ${infos[0]} " +
                    "cannot be referenced in a method annotated with ${infos[1]}."
            }
        },
        ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Variable declared in a class annotated with ${infos[0]} " +
                    "cannot be referenced in a method annotated with ${infos[1]}."
            }
        },
        ANNOTATED_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Variable annotated with ${infos[0]} " +
                    "cannot be referenced in a class annotated with ${infos[1]}."
            }
        },
        ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Variable declared in a class annotated with ${infos[0]}" +
                    " cannot be referenced in a class annotated with ${infos[1]}."
            }
        };

        abstract fun getErrorString(vararg infos: Any): String
    }
}
