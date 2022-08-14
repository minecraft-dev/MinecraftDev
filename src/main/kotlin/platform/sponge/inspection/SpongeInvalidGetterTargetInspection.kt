/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.uast.UMethod

class SpongeInvalidGetterTargetInspection : AbstractBaseUastLocalInspectionTool() {

    override fun getDisplayName() = "@Getter targeted method does not exist"

    override fun getStaticDescription() =
        "@Getter must target a method accessible from the event class of this listener"

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (!method.isValidSpongeListener()) {
            return null
        }

        val problems = mutableListOf<ProblemDescriptor>()
        for (parameter in method.uastParameters.drop(1)) {
            val getter = parameter.findAnnotation(SpongeConstants.GETTER_ANNOTATION) ?: continue
            if (getter.resolveSpongeGetterTarget() == null) {
                getter.findAttributeValue("value")?.sourcePsi?.let { problemAnchor ->
                    problems += manager.createProblemDescriptor(
                        problemAnchor,
                        this.staticDescription,
                        isOnTheFly,
                        null,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }
        return problems.toTypedArray()
    }
}
