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

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class MethodCallSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() =
        "Invalid usage of a @SideOnly method call"

    override fun buildErrorString(vararg infos: Any): String {
        val error = infos[0] as Error
        return error.getErrorString(*SideOnlyUtil.getSubArray(infos))
    }

    override fun getStaticDescription() =
        "Methods which are declared with a @SideOnly annotation can only be " +
            "used in matching @SideOnly classes and methods."

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val method = infos[3] as PsiMethod

        return if (method.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(method, "Remove @SideOnly annotation from method declaration")
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return
                }

                val referenceExpression = expression.methodExpression
                val qualifierExpression = referenceExpression.qualifierExpression

                // If this field is a @SidedProxy field, don't check. This is because people often are naughty and use the server impl as
                // the base class for their @SidedProxy class, and client extends it. this messes up our checks, so we will just assume the
                // right class is loaded for @SidedProxy's
                run skip@{
                    if (qualifierExpression is PsiReferenceExpression) {
                        val resolve = qualifierExpression.resolve() as? PsiField ?: return@skip

                        val resolveFieldModifierList = resolve.modifierList ?: return@skip

                        if (resolveFieldModifierList.findAnnotation(ForgeConstants.SIDED_PROXY_ANNOTATION) == null) {
                            return@skip
                        }

                        return
                    }
                }

                val declaration = referenceExpression.resolve() as? PsiMethod ?: return

                var elementSide = SideOnlyUtil.checkMethod(declaration)

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
                                referenceExpression.element,
                                Error.ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.annotation,
                                classSide.annotation,
                                declaration
                            )
                        } else {
                            registerError(
                                referenceExpression.element,
                                Error.ANNOTATED_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD,
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
                                    referenceExpression.element,
                                    Error.ANNOTATED_CLASS_METHOD_IN_UNANNOTATED_METHOD,
                                    elementSide.annotation, null,
                                    declaration
                                )
                            } else {
                                registerError(
                                    referenceExpression.element,
                                    Error.ANNOTATED_METHOD_IN_UNANNOTATED_METHOD,
                                    elementSide.annotation, null,
                                    declaration
                                )
                            }
                        }
                    } else {
                        if (inherited) {
                            registerError(
                                referenceExpression.element,
                                Error.ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.annotation,
                                methodSide.annotation,
                                declaration
                            )
                        } else {
                            registerError(
                                referenceExpression.element,
                                Error.ANNOTATED_METHOD_IN_CROSS_ANNOTATED_METHOD,
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
        ANNOTATED_METHOD_IN_UNANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method annotated with " + infos[0] + " cannot be referenced in an un-annotated method."
            }
        },
        ANNOTATED_CLASS_METHOD_IN_UNANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method declared in a class annotated with " + infos[0] +
                    " cannot be referenced in an un-annotated method."
            }
        },
        ANNOTATED_METHOD_IN_CROSS_ANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method annotated with " + infos[0] +
                    " cannot be referenced in a method annotated with " + infos[1] + "."
            }
        },
        ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method declared in a class annotated with " + infos[0] +
                    " cannot be referenced in a method annotated with " + infos[1] + "."
            }
        },
        ANNOTATED_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method annotated with " + infos[0] +
                    " cannot be referenced in a class annotated with " + infos[1] + "."
            }
        },
        ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method declared in a class annotated with " + infos[0] +
                    " cannot be referenced in a class annotated with " + infos[1] + "."
            }
        };

        abstract fun getErrorString(vararg infos: Any): String
    }
}
