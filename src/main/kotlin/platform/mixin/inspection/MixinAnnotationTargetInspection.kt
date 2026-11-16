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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.ide.nls.NlsMessages
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.parentOfType

class MixinAnnotationTargetInspection : MixinInspection() {
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
                    val targets = MixinAnnotationHandler.resolveTarget(parentAnnotation).ifEmpty { return }
                    val failure = targets.asSequence()
                        .mapNotNull {
                            (it as? MethodTargetMember)?.classAndMethod
                        }
                        // group by class
                        .groupBy { it.clazz.name }
                        .values.asSequence()
                        // for each class there must be at least one successful match
                        .mapNotNull classLoop@{ methods ->
                            methods
                                .map {
                                    AtResolver(annotation, it.clazz, it.method).isUnresolved() ?: return@classLoop null
                                }
                                .reduceOrNull(InsnResolutionInfo.Failure::combine)
                                ?: InsnResolutionInfo.Failure(
                                    MCDevBundle("inspection.mixin_annotation_target.unresolved_at"),
                                )
                        }
                        .reduceOrNull(InsnResolutionInfo.Failure::combine)
                    if (failure != null) {
                        addProblem(
                            annotation,
                            listOf(MCDevBundle("inspection.mixin_annotation_target.unresolved_at")),
                            failure,
                        )
                    }
                } else {
                    val handler = MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project) ?: return
                    val failure = handler.isUnresolved(annotation)
                    if (failure != null) {
                        addProblem(annotation, failure.messages, failure)
                    }
                }
            }

            private fun addProblem(
                annotation: PsiAnnotation,
                messages: Collection<String>,
                failure: InsnResolutionInfo.Failure,
            ) {
                val nameElement = annotation.nameReferenceElement ?: return
                val betterMessage = if (messages.size == 1) {
                    addFilterMessage(messages.single(), failure.filterStats)
                } else {
                    // One failure per line, the inspection tooltip renders messages starting with <html> as HTML
                    val summary = addFilterMessage(
                        MCDevBundle("inspection.mixin_annotation_target.unresolved_multiple"),
                        failure.filterStats,
                    )
                    HtmlBuilder()
                        .append(summary)
                        .append(HtmlChunk.ul().children(messages.map { HtmlChunk.li().addText(it) }))
                        .wrapWith("html")
                        .toString()
                }
                val quickFix = RemoveAnnotationQuickFix(annotation, annotation.parentOfType())
                holder.registerProblem(nameElement, betterMessage, quickFix)
            }

            private fun addFilterMessage(message: String, stats: Map<String, Int>): String {
                val nonZeroStats = stats.entries.filter { it.value != 0 }
                if (nonZeroStats.isEmpty()) {
                    return message
                }

                val parts = nonZeroStats.mapIndexed { i, (filter, count) ->
                    if (i == 0) {
                        MCDevBundle("inspection.mixin_annotation_target.filter_stats.first", count, filter)
                    } else {
                        MCDevBundle("inspection.mixin_annotation_target.filter_stats.more", count, filter)
                    }
                }
                return MCDevBundle(
                    "inspection.mixin_annotation_target.filter_stats",
                    message,
                    NlsMessages.formatAndList(parts),
                )
            }
        }
    }
}
