/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DESC
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByQualifiedName
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.findModule
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

object DescReference : AbstractMethodReference() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationParam(
            StandardPatterns.string().equalTo(DESC)
        )

    override val description = "method '%s'"

    override fun isValidAnnotation(name: String, project: Project) = name == DESC

    override fun parseSelector(context: PsiElement): DescSelector? {
        val annotation = context.parentOfType<PsiAnnotation>() ?: return null // @Desc
        return DescSelectorParser.descSelectorFromAnnotation(annotation)
    }

    override fun getTargets(context: PsiElement): Collection<ClassNode>? {
        return parseSelector(context)?.owners?.mapNotNull { internalName ->
            findClassNodeByQualifiedName(context.project, context.findModule(), internalName.replace('/', '.'))
        }
    }

    override fun addCompletionInfo(
        builder: LookupElementBuilder,
        context: PsiElement,
        targetMethodInfo: MemberReference
    ): LookupElementBuilder {
        return builder.withInsertHandler { insertionContext, _ ->
            insertionContext.laterRunnable =
                CompleteDescReference(insertionContext.editor, insertionContext.file, targetMethodInfo)
        }
    }

    override val requireDescriptor = true

    private class CompleteDescReference(
        private val editor: Editor,
        private val file: PsiFile,
        private val targetMethodInfo: MemberReference
    ) : Runnable {
        private fun PsiElementFactory.createAnnotationMemberValueFromText(
            text: String,
            context: PsiElement?
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
                            "\"${StringUtil.escapeStringCharacters(targetMethodInfo.name)}\"",
                            descAnnotation
                        )
                    )
                    val desc = targetMethodInfo.descriptor ?: return@runWriteAction
                    val argTypes = Type.getArgumentTypes(desc)
                    if (argTypes.isNotEmpty()) {
                        val argsText = if (argTypes.size == 1) {
                            "${argTypes[0].className.replace('$', '.')}.class"
                        } else {
                            "{${
                            argTypes.joinToString(", ") { type ->
                                "${type.className.replace('$', '.')}.class"
                            }
                            }}"
                        }
                        descAnnotation.setDeclaredAttributeValue(
                            "args",
                            elementFactory.createAnnotationMemberValueFromText(argsText, descAnnotation)
                        )
                    } else {
                        descAnnotation.setDeclaredAttributeValue("desc", null)
                    }
                    val returnType = Type.getReturnType(desc)
                    if (returnType.sort != Type.VOID) {
                        descAnnotation.setDeclaredAttributeValue(
                            "ret",
                            elementFactory.createAnnotationMemberValueFromText(
                                "${returnType.className.replace('$', '.')}.class",
                                descAnnotation
                            )
                        )
                    } else {
                        descAnnotation.setDeclaredAttributeValue("ret", null)
                    }
                }
            }
        }
    }
}
