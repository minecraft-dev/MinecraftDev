/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick.inspections

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.DataType
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickDescriptors
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickLiterals
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupRef
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTargetAnnotation
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTargetField
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTargetMethod
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.demonwav.mcdev.util.descriptor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class UnpickTargetMismatchInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : UnpickVisitor() {
            override fun visitTargetMethod(element: UnpickTargetMethod) {
                val descriptor = element.methodDescriptor?.text ?: return
                val parsed = UnpickDescriptors.parseMethod(descriptor) ?: return

                for (param in element.targetMethodParamList) {
                    val indexElement = param.index ?: continue
                    val index = UnpickLiterals.parseInt(indexElement.text) ?: continue
                    if (index < 0 || index >= parsed.parameters.size) {
                        holder.registerProblem(
                            indexElement,
                            MCDevBundle(
                                "inspection.unpick_target_mismatch.param_out_of_bounds",
                                index,
                                parsed.parameters.size,
                            ),
                        )
                        continue
                    }
                    val paramDescriptor = parsed.parameters[index]
                    checkCompatible(param.groupRef, paramDescriptor) { groupName, groupType ->
                        MCDevBundle(
                            "inspection.unpick_target_mismatch.param_incompatible",
                            index,
                            UnpickDescriptors.toJavaName(paramDescriptor),
                            groupName,
                            groupType.keyword,
                        )
                    }
                }

                for (ret in element.targetMethodReturnList) {
                    if (parsed.returnType == "V") {
                        holder.registerProblem(
                            ret.firstChild,
                            MCDevBundle("inspection.unpick_target_mismatch.void_return"),
                        )
                        continue
                    }
                    checkCompatible(ret.groupRef, parsed.returnType) { groupName, groupType ->
                        MCDevBundle(
                            "inspection.unpick_target_mismatch.return_incompatible",
                            UnpickDescriptors.toJavaName(parsed.returnType),
                            groupName,
                            groupType.keyword,
                        )
                    }
                }
            }

            override fun visitTargetField(element: UnpickTargetField) {
                val descriptorElement = element.fieldDescriptor ?: return
                val descriptor = descriptorElement.text
                if (UnpickDescriptors.validateField(descriptor) != null) {
                    return
                }

                val field = element.fieldName?.resolveField()
                val actualDescriptor = field?.descriptor
                if (field != null && actualDescriptor != null && actualDescriptor != descriptor) {
                    holder.registerProblem(
                        descriptorElement,
                        MCDevBundle("inspection.unpick_target_mismatch.field_descriptor", field.name, actualDescriptor),
                    )
                }

                checkCompatible(element.groupRef, descriptor) { groupName, groupType ->
                    MCDevBundle(
                        "inspection.unpick_target_mismatch.field_incompatible",
                        UnpickDescriptors.toJavaName(descriptor),
                        groupName,
                        groupType.keyword,
                    )
                }
            }

            override fun visitTargetAnnotation(element: UnpickTargetAnnotation) {
                val className = element.className ?: return
                val psiClass = className.resolveClass() ?: return
                if (!psiClass.isAnnotationType) {
                    holder.registerProblem(
                        className,
                        MCDevBundle("inspection.unpick_target_mismatch.not_annotation", className.qualifiedName),
                    )
                }
            }

            private inline fun checkCompatible(
                groupRef: UnpickGroupRef?,
                targetDescriptor: String,
                message: (String, DataType) -> String,
            ) {
                groupRef ?: return
                val targetType = DataType.fromDescriptor(targetDescriptor)
                val groupTypes = groupRef.resolveDefinitions().mapNotNull { it.dataType?.dataType }.distinct()
                for (groupType in groupTypes) {
                    if (targetType == null || !targetType.isTargetCompatibleWith(groupType)) {
                        holder.registerProblem(groupRef, message(groupRef.text, groupType))
                        return
                    }
                }
            }
        }
}
