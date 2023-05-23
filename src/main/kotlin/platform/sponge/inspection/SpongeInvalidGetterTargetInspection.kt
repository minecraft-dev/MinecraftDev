/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
        isOnTheFly: Boolean,
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
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    )
                }
            }
        }
        return problems.toTypedArray()
    }
}
