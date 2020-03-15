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

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class FieldDeclarationSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Invalid usage of @SideOnly in field declaration"

    override fun buildErrorString(vararg infos: Any): String {
        val error = infos[0] as Error
        return error.getErrorString(*SideOnlyUtil.getSubArray(infos))
    }

    override fun getStaticDescription(): String? {
        return "A field in a class annotated for one side cannot be declared as being in the other side. " +
            "For example, a class which is annotated as @SideOnly(Side.SERVER) cannot contain a field which is " +
            "annotated as @SideOnly(Side.CLIENT). Since a class that is annotated with @SideOnly brings " +
            "everything with it, @SideOnly annotated fields are usually useless"
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val field = infos[3] as PsiField

        return if (field.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(field, "Remove @SideOnly annotation from field")
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitField(field: PsiField) {
                val psiClass = field.containingClass ?: return

                if (!SideOnlyUtil.beginningCheck(field)) {
                    return
                }

                val fieldSide = SideOnlyUtil.checkField(field)
                if (fieldSide === Side.INVALID) {
                    return
                }

                val classSide = SideOnlyUtil.getSideForClass(psiClass)

                if (fieldSide !== Side.NONE && fieldSide !== classSide) {
                    if (classSide !== Side.NONE && classSide !== Side.INVALID) {
                        registerFieldError(
                            field,
                            Error.CLASS_CROSS_ANNOTATED,
                            fieldSide.annotation,
                            classSide.annotation,
                            field
                        )
                    } else if (classSide !== Side.NONE) {
                        registerFieldError(field, Error.CLASS_UNANNOTATED, fieldSide.annotation, null, field)
                    }
                }

                if (fieldSide === Side.NONE) {
                    return
                }

                if (field.type !is PsiClassType) {
                    return
                }

                val type = field.type as PsiClassType
                val fieldClass = type.resolve() ?: return

                val fieldClassSide = SideOnlyUtil.getSideForClass(fieldClass)

                if (fieldClassSide === Side.NONE || fieldClassSide === Side.INVALID) {
                    return
                }

                if (fieldClassSide !== fieldSide) {
                    registerFieldError(
                        field,
                        Error.FIELD_CROSS_ANNOTATED,
                        fieldClassSide.annotation,
                        fieldSide.annotation,
                        field
                    )
                }
            }
        }
    }

    enum class Error {
        CLASS_UNANNOTATED {
            override fun getErrorString(vararg infos: Any): String {
                return "Field with type annotation ${infos[1]} cannot be declared in an un-annotated class"
            }
        },
        CLASS_CROSS_ANNOTATED {
            override fun getErrorString(vararg infos: Any): String {
                return "Field annotated with ${infos[0]} cannot be declared inside a class annotated with ${infos[1]}."
            }
        },
        FIELD_CROSS_ANNOTATED {
            override fun getErrorString(vararg infos: Any): String {
                return "Field with type annotation ${infos[0]} cannot be declared as ${infos[1]}."
            }
        };

        abstract fun getErrorString(vararg infos: Any): String
    }
}
