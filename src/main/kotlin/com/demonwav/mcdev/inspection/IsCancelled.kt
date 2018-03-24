/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class IsCancelled(
    fix: (ProblemDescriptor) -> Unit,
    val errorString: String
) {
    val buildFix: InspectionGadgetsFix

    init {
        this.buildFix = object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) = fix(descriptor)

            @Nls(capitalization = Nls.Capitalization.Sentence)
            override fun getName() = "Simplify"

            @Nls(capitalization = Nls.Capitalization.Sentence)
            override fun getFamilyName() = "Useless IsCancelled Check"
        }
    }
}
