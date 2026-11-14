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

package com.demonwav.mcdev.platform.fabric.inspection

import com.demonwav.mcdev.platform.fabric.reference.EntryPointReference
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.equivalentTo
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonLiteral
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.parentOfType

class FabricEntrypointsInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (holder.file.name == FabricConstants.FABRIC_MOD_JSON) {
            return Visitor(holder)
        }
        return PsiElementVisitor.EMPTY_VISITOR
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        if (file.name == FabricConstants.FABRIC_MOD_JSON) {
            return super.processFile(file, manager)
        }
        return emptyList()
    }

    private class Visitor(private val holder: ProblemsHolder) : JsonElementVisitor() {

        override fun visitStringLiteral(literal: JsonStringLiteral) {
            for (reference in literal.references) {
                if (reference !is EntryPointReference.Reference) {
                    continue
                }

                val resolved = reference.multiResolve(false)
                if (resolved.size > 1) {
                    holder.registerProblem(
                        literal,
                        "Ambiguous member reference",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        reference.rangeInElement,
                    )
                }

                if (!reference.isFinalPart) {
                    continue
                }

                when (val element = resolved.singleOrNull()?.element) {
                    is PsiClass if !literal.text.contains("::") -> {
                        val (propertyKey, expectedType) = findEntrypointKeyAndType(literal)
                        if (propertyKey != null && expectedType != null &&
                            !isEntrypointOfCorrectType(element, propertyKey)
                        ) {
                            holder.registerProblem(
                                literal,
                                "'$propertyKey' entrypoints must implement $expectedType",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                reference.rangeInElement,
                            )
                        } else if (element.constructors.isNotEmpty() &&
                            element.constructors.find { !it.hasParameters() } == null
                        ) {
                            holder.registerProblem(
                                literal,
                                "Entrypoint class must have an empty constructor",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                reference.rangeInElement,
                            )
                        }
                    }

                    is PsiMethod -> {
                        if (element.hasParameters()) {
                            holder.registerProblem(
                                literal,
                                "Entrypoint method must have no parameters",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                reference.rangeInElement,
                            )
                        }

                        if (!element.hasModifierProperty(PsiModifier.PUBLIC)) {
                            holder.registerProblem(
                                literal,
                                "Entrypoint method must be public",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                reference.rangeInElement,
                            )
                        }

                        if (!element.hasModifierProperty(PsiModifier.STATIC)) {
                            val clazz = element.containingClass
                            if (clazz != null && clazz.constructors.isNotEmpty() &&
                                clazz.constructors.find { !it.hasParameters() } == null
                            ) {
                                holder.registerProblem(
                                    literal,
                                    "Entrypoint instance method class must have an empty constructor",
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    reference.rangeInElement,
                                )
                            }
                        }
                    }

                    is PsiField -> {
                        if (!element.hasModifierProperty(PsiModifier.PUBLIC)) {
                            holder.registerProblem(
                                literal,
                                "Entrypoint field must be public",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                reference.rangeInElement,
                            )
                        }

                        val (propertyKey, expectedType) = findEntrypointKeyAndType(literal)
                        val fieldTypeClass = (element.type as? PsiClassType)?.resolve()
                        if (propertyKey != null && fieldTypeClass != null && expectedType != null &&
                            !isEntrypointOfCorrectType(fieldTypeClass, propertyKey)
                        ) {
                            holder.registerProblem(
                                literal,
                                "'$propertyKey' entrypoints must be of type $expectedType",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                reference.rangeInElement,
                            )
                        }
                    }
                }
            }
        }

        private fun findEntrypointKeyAndType(literal: JsonLiteral): Pair<String?, String?> {
            val propertyKey = when (val parent = literal.parent) {
                is JsonArray -> (parent.parent as? JsonProperty)?.name
                is JsonProperty -> parent.parentOfType<JsonProperty>()?.name
                else -> null
            }
            val expectedType = propertyKey?.let { FabricConstants.ENTRYPOINT_BY_TYPE[it] }
            return propertyKey to expectedType
        }

        private fun isEntrypointOfCorrectType(element: PsiClass, type: String): Boolean {
            val entrypointClass = FabricConstants.ENTRYPOINT_BY_TYPE[type]
                ?: return false
            val clazz = JavaPsiFacade.getInstance(element.project).findClass(entrypointClass, element.resolveScope)
            return clazz != null && (element.equivalentTo(clazz) || element.isInheritor(clazz, true))
        }
    }
}
