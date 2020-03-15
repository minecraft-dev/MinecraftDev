/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.siyeh.ig.InspectionGadgetsFix

class IsCancelled(
    fix: (ProblemDescriptor) -> Unit,
    val errorString: String
) {
    val buildFix: InspectionGadgetsFix

    init {
        this.buildFix = object : InspectionGadgetsFix() {
            override fun doFix(project: Project, descriptor: ProblemDescriptor) = fix(descriptor)
            override fun getName() = "Simplify"
            override fun getFamilyName() = "Useless IsCancelled Check"
        }
    }
}
