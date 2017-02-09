/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class IsCancelled(val buildFix: InspectionGadgetsFix, val errorString: String) {

    class IsCancelledBuilder {
        private var fix: InspectionGadgetsFix? = null
        private var errorString: String? = null

        fun setFix(fix: DoFix): IsCancelledBuilder {
            this.fix = object : InspectionGadgetsFix() {
                override fun doFix(project: Project, descriptor: ProblemDescriptor) = fix.doFix(descriptor)

                @Nls
                override fun getName() = "Simplify"

                @Nls
                override fun getFamilyName() = "Useless Is Cancelled Check"
            }
            return this
        }

        fun setErrorString(errorString: String): IsCancelledBuilder {
            this.errorString = errorString
            return this
        }

        fun build(): IsCancelled {
            return IsCancelled(fix!!, errorString!!)
        }
    }

    interface DoFix {
        fun doFix(descriptor: ProblemDescriptor)
    }

    companion object {
        @JvmStatic
        fun builder(): IsCancelledBuilder {
            return IsCancelledBuilder()
        }
    }
}
