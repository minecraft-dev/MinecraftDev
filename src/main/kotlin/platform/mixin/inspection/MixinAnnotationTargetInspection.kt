/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.parentOfType

class MixinAnnotationTargetInspection : MixinInspection() {
    override fun getStaticDescription() = "Verifies targets of mixin annotations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                val qName = annotation.qualifiedName ?: return
                if (qName == AT) {
                    var parentAnnotation = annotation.parentOfType<PsiAnnotation>()
                    while (parentAnnotation != null) {
                        if (MixinAnnotationHandler.forMixinAnnotation(parentAnnotation) != null) {
                            break
                        }
                        parentAnnotation = parentAnnotation.parentOfType()
                    }
                    if (parentAnnotation == null) {
                        return
                    }
                    val targetMembers = MixinAnnotationHandler.resolveTarget(parentAnnotation).ifEmpty { return }
                    val failure = targetMembers.asSequence()
                        .mapNotNull {
                            val member = it as? MethodTargetMember
                            val classAndMethod = member?.classAndMethod
                            val selector = (it as? ContextAwareMethodTargetMember)?.selector
                            if (classAndMethod != null) classAndMethod to selector else null
                        }
                        // group by class
                        .groupBy { it.first.clazz.name }
                        .values.asSequence()
                        // for each class there must be at least one successful match
                        .mapNotNull classLoop@{ methods ->
                            methods
                                .map {
                                    AtResolver(annotation, it.first.clazz, it.first.method, it.second).isUnresolved() ?: return@classLoop null
                                }
                                .reduceOrNull(InsnResolutionInfo.Failure::combine) ?: InsnResolutionInfo.Failure()
                        }
                        .reduceOrNull(InsnResolutionInfo.Failure::combine)
                    if (failure != null) {
                        addProblem(annotation, "Could not resolve @At target", failure)
                    }
                } else {
                    val handler = MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project) ?: return
                    val failure = handler.isUnresolved(annotation)
                    if (failure != null) {
                        val message = handler.createUnresolvedMessage(annotation) ?: return
                        addProblem(annotation, message, failure)
                    }
                }
            }

            private fun addProblem(annotation: PsiAnnotation, message: String, failure: InsnResolutionInfo.Failure) {
                val nameElement = annotation.nameReferenceElement ?: return
                val betterMessage = addFilterMessage(message, failure.filterStats)
                val quickFix = RemoveAnnotationQuickFix(annotation, annotation.parentOfType())
                holder.registerProblem(nameElement, betterMessage, quickFix)
            }

            private fun addFilterMessage(message: String, stats: Map<String, Int>): String {
                val statsItr = stats.entries.asSequence().filter { it.value != 0 }.iterator()
                if (!statsItr.hasNext()) {
                    return message
                }

                return buildString {
                    append(message)
                    append(" (")
                    append(statsItr.next().let { (k, v) -> "$v filtered out by $k" })
                    for ((k, v) in statsItr) {
                        append(", $v more by $k")
                    }
                    append(')')
                }
            }
        }
    }
}
