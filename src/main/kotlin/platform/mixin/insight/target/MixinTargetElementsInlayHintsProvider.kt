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

package com.demonwav.mcdev.platform.mixin.insight.target

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.LibraryModIdProvider
import com.demonwav.mcdev.platform.mixin.action.FindMixinsAction
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByPsiClass
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.filename.UniqueNameBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.createSmartPointer
import java.awt.Font
import javax.swing.JPanel

// TODO: migrate to declarative inlay hints once they support custom icons. See IJPL-29654.
// When this is possible, use OwnBypassCollector
// TODO: add settings
@Suppress("UnstableApiUsage")
class MixinTargetElementsInlayHintsProvider : InlayHintsProvider<NoSettings> {
    companion object {
        private val KEY = SettingsKey<NoSettings>("mcdev.mixin.target.elements")
        private val MIXIN_ANNOTATIONS_TARGETING_THIS_ELEMENT_KEY =
            Key<AnnotationsTargetingThisElement>("mcdev.mixin.annotations.targeting.this.element")

        fun createDefaultTargetInlay(
            handler: MixinAnnotationHandler,
            context: MixinAnnotationHandler.TargetInlayContext
        ): MixinAnnotationHandler.TargetInlayProperties? {
            val annotationName = context.annotation.nameReferenceElement?.text ?: return null
            val placement = if (context.targetElement is PsiMember) {
                MixinAnnotationHandler.TargetInlayPlacement.PREVIOUS_LINE
            } else {
                MixinAnnotationHandler.TargetInlayPlacement.BEFORE
            }
            return MixinAnnotationHandler.TargetInlayProperties(
                context.targetElement,
                placement,
                handler.targetIcon,
                buildString {
                    append("@$annotationName : ${context.mixinName}")
                    if (context.libraryModId != null) {
                        append(" (${context.libraryModId})")
                    }
                },
            )
        }
    }

    override val key = KEY
    override val name: String
        get() = MCDevBundle("mixin.inlay.target.elements.name")
    override val previewText = null

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): FactoryInlayHintsCollector {
        val project = file.project
        val modCount = PsiModificationTracker.getInstance(project).modificationCount
        val document = editor.document

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val annotationsTargetingThisElement = element.getUserData(MIXIN_ANNOTATIONS_TARGETING_THIS_ELEMENT_KEY)
                if (annotationsTargetingThisElement != null && annotationsTargetingThisElement.modCount == modCount) {
                    applyInlayHints(
                        element,
                        editor,
                        sink,
                        annotationsTargetingThisElement.annotations
                    )
                    return true
                }

                val targetClass = element.findContainingClass() ?: return true
                val targetClassNode = findClassNodeByPsiClass(targetClass) ?: return true
                val mixins = FindMixinsAction.findMixins(targetClass, project) ?: return true
                val annotationsTargetingElements = mutableMapOf<PsiElement, MutableList<Pair<Int, PsiAnnotation>>>()
                val uniqueMixinShortNames = mutableMapOf<String?, MutableMap<String, Boolean>>()
                val uniqueMixinNameBuilders = mutableMapOf<String?, UniqueNameBuilder<PsiClass>>()
                for (mixin in mixins) {
                    val mixinShortName = mixin.name ?: continue
                    val mixinQName = mixin.qualifiedName ?: continue
                    val libraryModId = getLibraryModId(project, mixin)
                    uniqueMixinShortNames.getOrPut(libraryModId) { mutableMapOf() }
                        .merge(mixinShortName, true) { _, _ -> false }
                    uniqueMixinNameBuilders.getOrPut(libraryModId) { UniqueNameBuilder("", ".") }
                        .addPath(mixin, "/" + mixinQName.replace('.', '/'))
                    for (member in mixin.children) {
                        if (member !is PsiMember) {
                            continue
                        }
                        for (annotation in member.annotations) {
                            val qName = annotation.qualifiedName ?: continue
                            val mixinHandler = MixinAnnotationHandler.forMixinAnnotation(qName, project) ?: continue
                            for ((index, navigationResult) in mixinHandler
                                .resolveForNavigation(annotation, targetClassNode)
                                .withIndex()
                            ) {
                                annotationsTargetingElements.getOrPut(navigationResult) { mutableListOf() } +=
                                    index to annotation
                            }
                        }
                    }
                }

                targetClass.accept(object : JavaRecursiveElementWalkingVisitor() {
                    override fun visitElement(element: PsiElement) {
                        element.putUserData(
                            MIXIN_ANNOTATIONS_TARGETING_THIS_ELEMENT_KEY,
                            AnnotationsTargetingThisElement(
                                annotationsTargetingElements[element]?.mapNotNull { (navigationIndex, annotation) ->
                                    val mixinClass = annotation.findContainingClass() ?: return@mapNotNull null
                                    val mixinShortName = mixinClass.name ?: return@mapNotNull null
                                    val libraryModId = getLibraryModId(project, mixinClass)
                                    val uniqueMixinName =
                                        if (uniqueMixinShortNames[libraryModId]?.get(mixinShortName) == true) {
                                            mixinShortName
                                        } else {
                                            uniqueMixinNameBuilders[libraryModId]?.getShortPath(mixinClass)
                                                ?: return@mapNotNull null
                                        }
                                    Annotation(
                                        annotation.createSmartPointer(project),
                                        uniqueMixinName,
                                        libraryModId,
                                        navigationIndex
                                    )
                                } ?: emptyList(),
                                modCount,
                            )
                        )
                        super.visitElement(element)
                    }

                    override fun visitClass(aClass: PsiClass) {
                        // don't recurse into inner classes
                        if (aClass == targetClass) {
                            super.visitClass(aClass)
                        }
                    }
                })

                applyInlayHints(
                    element,
                    editor,
                    sink,
                    element.getUserData(MIXIN_ANNOTATIONS_TARGETING_THIS_ELEMENT_KEY)?.annotations ?: emptyList()
                )

                return true
            }

            private fun applyInlayHints(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink,
                annotationsTargetingThisElement: List<Annotation>
            ) {
                for (
                    (annotationPtr, uniqueMixinName, libraryModId, navigationIndex) in annotationsTargetingThisElement
                ) {
                    val annotation = annotationPtr.element ?: continue
                    val qName = annotation.qualifiedName ?: continue
                    val annotationHandler = MixinAnnotationHandler.forMixinAnnotation(qName, project) ?: continue
                    val inlayProperties = annotationHandler.createTargetInlay(
                        MixinAnnotationHandler.TargetInlayContext(
                            annotation,
                            element,
                            uniqueMixinName,
                            libraryModId,
                            navigationIndex
                        )
                    ) ?: continue
                    val presentation = factory.inset(
                        factory.seq(
                            factory.icon(inlayProperties.icon),
                            factory.inset(factory.text(inlayProperties.text), left = 1)
                        ),
                        left = 1,
                        right = 1
                    )
                    when (inlayProperties.placement) {
                        MixinAnnotationHandler.TargetInlayPlacement.BEFORE -> {
                            sink.addInlineElement(
                                inlayProperties.anchor.textRange.startOffset,
                                relatesToPrecedingText = false,
                                presentation,
                                placeAtTheEndOfLine = false,
                            )
                        }
                        MixinAnnotationHandler.TargetInlayPlacement.AFTER -> {
                            sink.addInlineElement(
                                inlayProperties.anchor.textRange.endOffset,
                                relatesToPrecedingText = true,
                                presentation,
                                placeAtTheEndOfLine = false,
                            )
                        }
                        MixinAnnotationHandler.TargetInlayPlacement.END_OF_LINE -> {
                            sink.addInlineElement(
                                inlayProperties.anchor.textRange.endOffset,
                                relatesToPrecedingText = false,
                                presentation,
                                placeAtTheEndOfLine = true,
                            )
                        }
                        MixinAnnotationHandler.TargetInlayPlacement.PREVIOUS_LINE -> {
                            val offset = inlayProperties.anchor.textRange.startOffset
                            val lineNumber = document.getLineNumber(offset)
                            val lineStartOffset = document.getLineStartOffset(lineNumber)
                            val startOffset = lineStartOffset + document.charsSequence.subSequence(
                                lineStartOffset,
                                document.getLineEndOffset(lineNumber)
                            ).indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                            sink.addBlockElement(
                                offset,
                                relatesToPrecedingText = false,
                                showAbove = true,
                                priority = 0,
                                factory.inset(
                                    presentation,
                                    left = EditorUtil.textWidth(
                                        editor, document.charsSequence, lineStartOffset, startOffset, Font.PLAIN, 0
                                    )
                                ),
                            )
                        }
                        MixinAnnotationHandler.TargetInlayPlacement.NEXT_LINE -> {
                            // intellij has a bug where it will only show a maximum of 1 showBelow inlay hints, so we
                            // will use showAbove the next line instead. See InlayHintsPass.addBlockHints
                            val offset = inlayProperties.anchor.textRange.endOffset
                            val lineNumber = document.getLineNumber(offset)
                            val lineStartOffset = document.getLineStartOffset(lineNumber)
                            val startOffset = lineStartOffset + document.charsSequence.subSequence(
                                lineStartOffset,
                                document.getLineEndOffset(lineNumber)
                            ).indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                            val adjustedPresentation = factory.inset(
                                presentation,
                                left = EditorUtil.textWidth(
                                    editor, document.charsSequence, lineStartOffset, startOffset, Font.PLAIN, 0
                                )
                            )
                            if (lineNumber == document.lineCount - 1) {
                                sink.addBlockElement(
                                    offset,
                                    relatesToPrecedingText = true,
                                    showAbove = false,
                                    priority = 0,
                                    adjustedPresentation,
                                )
                            } else {
                                sink.addBlockElement(
                                    document.getLineStartOffset(lineNumber + 1),
                                    relatesToPrecedingText = true,
                                    showAbove = true,
                                    priority = 0,
                                    adjustedPresentation,
                                )
                            }
                        }
                    }
                }
            }

            private fun getLibraryModId(project: Project, mixin: PsiClass): String? {
                val mixinFile = mixin.containingFile?.virtualFile ?: return null
                val library = (LibraryUtil.findLibraryEntry(mixinFile, project) as? LibraryOrderEntry)?.library
                    ?: return null
                return LibraryModIdProvider.getModId(project, library)
            }
        }
    }

    override fun createConfigurable(settings: NoSettings) = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener) = JPanel()
    }

    private class AnnotationsTargetingThisElement(
        val annotations: List<Annotation>,
        val modCount: Long,
    )

    private data class Annotation(
        val annotation: SmartPsiElementPointer<PsiAnnotation>,
        val uniqueMixinName: String,
        val libraryModId: String?,
        val navigationIndex: Int,
    )
}
