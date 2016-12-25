/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiMethod

class MixinOverwriteInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getStaticDescription() = "Reports related to Mixin @Overwrites"

    override fun checkMethod(method: PsiMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val modifiers = method.modifierList

        // Check if the method is an @Overwrite
        if (modifiers.findAnnotation(MixinConstants.Annotations.OVERWRITE) == null) {
            return null
        }

        val identifier = method.nameIdentifier ?: return null

        val psiClass = getClassOfElement(method) ?: return null
        val targets = MixinUtils.getAllMixedClasses(psiClass).values

        val inad = method.internalNameAndDescriptor

        when (targets.size) {
            0 -> return null
            1 -> targets.single().findMethodsByInternalNameAndDescriptor(inad).findAny().orElse(null)
            else -> {
                // TODO: Improve detection of valid target methods in Mixins with multiple targets
                targets.stream()
                        .flatMap { it.findMethodsByInternalNameAndDescriptor(inad) }
                        .findAny().orElse(null)
            }
        } ?: return arrayOf(manager.createProblemDescriptor(identifier, "Cannot resolve method '${method.name}' in target class",
                null as LocalQuickFix?, ProblemHighlightType.GENERIC_ERROR, isOnTheFly))

        // TODO: Verify method modifiers?
        return null
    }

}
