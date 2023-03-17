/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.uast.UClass

class NonJavaMixinInspection : AbstractBaseUastLocalInspectionTool(UClass::class.java) {
    override fun getDisplayName() = "Mixin is not written in Java"
    override fun getStaticDescription() =
        "<html>Mixins should be written in Java. See <a href=\"$RELEVANT_ISSUE\">this Mixin issue</a></html>"

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val sourcePsi = aClass.sourcePsi ?: return null
        if (sourcePsi.language == JavaLanguage.INSTANCE) {
            return null
        }
        val isMixin = aClass.uAnnotations.any { ann -> ann.qualifiedName == MixinConstants.Annotations.MIXIN }
        if (!isMixin) {
            return null
        }
        val problem = manager.createProblemDescriptor(
            aClass.uastAnchor?.sourcePsi ?: sourcePsi,
            this.staticDescription,
            isOnTheFly,
            null,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )
        return arrayOf(problem)
    }

    companion object {
        private const val RELEVANT_ISSUE = "https://github.com/SpongePowered/Mixin/issues/245"
    }
}
