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

import com.intellij.psi.PsiClass
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class NestedClassSideOnlyInspection : BaseInspection() {

    @Nls
    override fun getDisplayName() = "Invalid usage of @SideOnly in nested class declaration"

    override fun buildErrorString(vararg infos: Any) =
        "A nested class cannot declare a side that is different from the parent class." +
            "\nEither remove the nested class's @SideOnly annotation, or change it to match it's parent's side."

    override fun getStaticDescription(): String? {
        return "Classes which are annotated with @SideOnly cannot contain any nested classes which are " +
            "annotated with a different @SideOnly annotation. Since a class that is annotated with @SideOnly " +
            "brings everything with it, @SideOnly annotated nested classes are usually useless."
    }

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        val psiClass = infos[0] as PsiClass

        return if (psiClass.isWritable) {
            RemoveAnnotationInspectionGadgetsFix(psiClass, "Remove @SideOnly annotation from nested class")
        } else {
            null
        }
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (aClass.parent == null) {
                    return
                }

                aClass.nameIdentifier ?: return

                if (!SideOnlyUtil.beginningCheck(aClass)) {
                    return
                }

                val classHierarchyList = SideOnlyUtil.checkClassHierarchy(aClass)

                // The class lists are ordered from lowest to highest in the hierarchy - that is the first element in the list
                // is the most nested class, and the last element in the list is the top level class
                //
                // In this case, the higher-level classes take precedence, so if a class is annotated as @SideOnly.CLIENT and a nested class is
                // annotated as @SideOnly.SERVER, the nested class is the class that is in error, not the top level class
                var currentSide = Side.NONE
                for (pair in classHierarchyList) {
                    if (currentSide === Side.NONE) {
                        // If currentSide is NONE, then a class hasn't declared yet what it is
                        if (pair.first !== Side.NONE && pair.first !== Side.INVALID) {
                            currentSide = pair.first
                        } else {
                            // We are only worried about this class
                            return
                        }
                    } else if (pair.first !== Side.NONE && pair.first !== Side.INVALID) {
                        if (pair.first !== currentSide) {
                            registerClassError(aClass, aClass)
                        } else {
                            return
                        }
                    }
                }
            }
        }
    }
}
