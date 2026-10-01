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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DESC
import com.demonwav.mcdev.platform.mixin.util.canonicalName
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByQualifiedName
import com.demonwav.mcdev.platform.mixin.util.memberReference
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

object DescReference : AbstractMethodReference() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationParam(
            StandardPatterns.string().equalTo(DESC),
        )

    override val description = "method '%s'"

    override fun isValidAnnotation(name: String, project: Project) = name == DESC

    override fun parseSelector(context: PsiElement): DescSelector? {
        val annotation = context.parentOfType<PsiAnnotation>() ?: return null // @Desc
        return DescSelectorParser.Util.descSelectorFromAnnotation(annotation)
    }

    override fun getTargets(context: PsiElement): Collection<ClassNode>? {
        return parseSelector(context)?.owners?.mapNotNull { internalName ->
            findClassNodeByQualifiedName(context.project, context.findModule(), internalName.replace('/', '.'))
        }
    }

    override fun getSuggestions(context: PsiElement, targets: Collection<ClassNode>): Array<Any> {
        val result = mutableListOf<LookupElement>()

        val allMethods = targets.asSequence()
            .flatMap { target ->
                target.methods.asSequence().map { ClassAndMethodNode(target, it) }
            }
            .groupBy { it.method.memberReference }
            .asSequence()
            .filter { it.value.size >= targets.size }
            .map { it.value.first() }
            .toList()

        for (m in allMethods) {
            val builder = methodLookupBuilder(m, MemberReference(m.method.name), context)
            result.add(addCompletionInfo(builder, m.method))
        }

        return result.toTypedArray()
    }

    private fun addCompletionInfo(
        builder: LookupElementBuilder,
        targetMethod: MethodNode,
    ): LookupElementBuilder {
        return builder.withInsertHandler { insertionContext, _ ->
            insertionContext.laterRunnable =
                CompleteDescReference(insertionContext.editor, insertionContext.file, targetMethod)
        }
    }

    private class CompleteDescReference(
        private val editor: Editor,
        private val file: PsiFile,
        private val targetMethod: MethodNode,
    ) : Runnable {
        private fun PsiElementFactory.createAnnotationMemberValueFromText(
            text: String,
            context: PsiElement?,
        ): PsiAnnotationMemberValue {
            val annotation = this.createAnnotationFromText("@Foo($text)", context)
            return annotation.findDeclaredAttributeValue("value")!!
        }

        override fun run() {
            // Commit changes made by code completion
            PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)

            // Run command to replace PsiElement
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    val descAnnotation = file.findElementAt(editor.caretModel.offset)?.parentOfType<PsiAnnotation>()
                        ?: return@runWriteAction
                    val project = editor.project ?: return@runWriteAction
                    val elementFactory = JavaPsiFacade.getElementFactory(project)
                    descAnnotation.setDeclaredAttributeValue(
                        "value",
                        elementFactory.createExpressionFromText(
                            "\"${StringUtil.escapeStringCharacters(targetMethod.name)}\"",
                            descAnnotation,
                        ),
                    )
                    val desc = targetMethod.desc
                    val argTypes = Type.getArgumentTypes(desc)
                    if (argTypes.isNotEmpty()) {
                        val argsText = if (argTypes.size == 1) {
                            "${argTypes[0].canonicalName}.class"
                        } else {
                            "{${
                            argTypes.joinToString(", ") { type ->
                                "${type.canonicalName}.class"
                            }
                            }}"
                        }
                        descAnnotation.setDeclaredAttributeValue(
                            "args",
                            elementFactory.createAnnotationMemberValueFromText(argsText, descAnnotation),
                        )
                    } else {
                        descAnnotation.setDeclaredAttributeValue("desc", null)
                    }
                    val returnType = Type.getReturnType(desc)
                    if (returnType.sort != Type.VOID) {
                        descAnnotation.setDeclaredAttributeValue(
                            "ret",
                            elementFactory.createAnnotationMemberValueFromText(
                                "${returnType.canonicalName}.class",
                                descAnnotation,
                            ),
                        )
                    } else {
                        descAnnotation.setDeclaredAttributeValue("ret", null)
                    }
                }
            }
        }
    }
}
