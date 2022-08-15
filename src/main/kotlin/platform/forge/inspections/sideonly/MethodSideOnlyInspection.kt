/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class MethodSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Invalid usage of @SideOnly in method declaration"

    override fun buildErrorString(vararg infos: Any): String {
        val error = infos[0] as Error
        return error.getErrorString(*SideOnlyUtil.getSubArray(infos))
    }

    override fun getStaticDescription(): String? {
        return "A method in a class annotated for one side cannot be declared as being in the other side. " +
            "For example, a class which is annotated as @SideOnly(Side.SERVER) cannot contain a method which " +
            "is annotated as @SideOnly(Side.CLIENT). Since a class that is annotated with @SideOnly brings " +
            "everything with it, @SideOnly annotated methods are usually useless"
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val error = infos[0] as Error
        val annotation = infos[3] as PsiAnnotation

        return if (annotation.isWritable && error === Error.METHOD_IN_WRONG_CLASS) {
            RemoveAnnotationInspectionGadgetsFix(annotation, "Remove @SideOnly annotation from method")
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitMethod(method: PsiMethod) {
                val psiClass = method.containingClass ?: return

                if (!SideOnlyUtil.beginningCheck(method)) {
                    return
                }

                val (methodAnnotation, methodSide) = SideOnlyUtil.checkMethod(method)
                if (methodAnnotation == null) {
                    return
                }

                val resolve = (method.returnType as? PsiClassType)?.resolve()

                val (returnAnnotation, returnSide) =
                    if (resolve == null) null to Side.NONE else SideOnlyUtil.getSideForClass(resolve)
                if (returnAnnotation != null && returnSide !== Side.NONE && returnSide !== Side.INVALID &&
                    returnSide !== methodSide && methodSide !== Side.NONE && methodSide !== Side.INVALID
                ) {
                    registerMethodError(
                        method,
                        Error.RETURN_TYPE_ON_WRONG_METHOD,
                        methodAnnotation.renderSide(methodSide),
                        returnAnnotation.renderSide(returnSide),
                        method.getAnnotation(methodAnnotation.annotationName)
                    )
                }

                for ((classAnnotation, classSide) in SideOnlyUtil.checkClassHierarchy(psiClass)) {
                    if (classAnnotation != null && classSide !== Side.NONE && classSide !== Side.INVALID) {
                        if (
                            methodSide !== classSide &&
                            methodSide !== Side.NONE &&
                            methodSide !== Side.INVALID
                        ) {
                            registerMethodError(
                                method,
                                Error.METHOD_IN_WRONG_CLASS,
                                methodAnnotation.renderSide(methodSide),
                                classAnnotation.renderSide(classSide),
                                method.getAnnotation(methodAnnotation.annotationName)
                            )
                        }
                        if (returnAnnotation != null && returnSide !== Side.NONE && returnSide !== Side.INVALID) {
                            if (returnSide !== classSide) {
                                registerMethodError(
                                    method,
                                    Error.RETURN_TYPE_IN_WRONG_CLASS,
                                    classAnnotation.renderSide(classSide),
                                    returnAnnotation.renderSide(returnSide),
                                    method.getAnnotation(methodAnnotation.annotationName)
                                )
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    enum class Error {
        METHOD_IN_WRONG_CLASS {
            override fun getErrorString(vararg infos: Any): String {
                return "Method annotated with " + infos[0] +
                    " cannot be declared inside a class annotated with " + infos[1] + "."
            }
        },
        RETURN_TYPE_ON_WRONG_METHOD {
            override fun getErrorString(vararg infos: Any): String {
                return "Method annotated with " + infos[0] +
                    " cannot return a type annotated with " + infos[1] + "."
            }
        },
        RETURN_TYPE_IN_WRONG_CLASS {
            override fun getErrorString(vararg infos: Any): String {
                return "Method in a class annotated with " + infos[0] +
                    " cannot return a type annotated with " + infos[1] + "."
            }
        };

        abstract fun getErrorString(vararg infos: Any): String
    }
}
