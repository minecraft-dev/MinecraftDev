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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.InsnInjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.CALLBACK_INFO
import com.demonwav.mcdev.platform.mixin.util.findDelegateConstructorCall
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.platform.mixin.util.isMixinExtrasSugar
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.SequencedSet
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findKeyword
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.synchronize
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.template.impl.VariableNode
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiType
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.startOffset
import com.siyeh.ig.psiutils.VariableNameGenerator
import org.objectweb.asm.Opcodes

class InvalidInjectorMethodSignatureInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports problems related to the method signature of Mixin injectors"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            val identifier = method.nameIdentifier ?: return
            val modifiers = method.modifierList
            val parameters = method.parameterList

            val (annotation, handler) = modifiers.annotations.firstNotNullOfOrNull { annotation ->
                (MixinAnnotationHandler.forMixinAnnotation(annotation, annotation.project)
                    as? InjectorAnnotationHandler)?.let { annotation to it }
            } ?: return

            val targetMethods = annotation.findContainingClass()?.mixinTargets?.flatMap { targetClass ->
                handler.resolveTarget(annotation, targetClass).map { it.classAndMethod }
            } ?: return

            val matchesByMethod = targetMethods.asSequence()
                .mapNotNull { classAndMethod ->
                    if (handler is InsnInjectorAnnotationHandler) {
                        handler.resolveInstructions(annotation, classAndMethod.clazz, classAndMethod.method)
                            .takeUnless { it.isEmpty() }
                            ?.let { classAndMethod to it }
                    } else {
                        classAndMethod to emptyList()
                    }
                }
                .toMap()
                .ifEmpty { return }

            val hasDisallowedInsns = handler is InsnInjectorAnnotationHandler && matchesByMethod.values.asSequence()
                .flatten().any { !handler.isInsnAllowed(it.insn, it.decorations) }
            if (hasDisallowedInsns) {
                return
            }

            val requiredStaticness = matchesByMethod.asSequence()
                .mapNotNull { (targetMethod, matches) ->
                    var shouldBeStatic = targetMethod.method.hasAccess(Opcodes.ACC_STATIC)

                    if (!shouldBeStatic && targetMethod.method.isConstructor) {
                        // before the superclass constructor call, everything must be static
                        val methodInsns = targetMethod.method.instructions
                        val delegateCtorCall = targetMethod.method.findDelegateConstructorCall()
                        if (methodInsns != null && delegateCtorCall != null) {
                            shouldBeStatic = matches.any {
                                methodInsns.indexOf(it.insn) <= methodInsns.indexOf(delegateCtorCall)
                            }
                        }
                    }

                    when {
                        shouldBeStatic -> true
                        handler.canAlwaysBeStatic(method) -> null
                        else -> false
                    }
                }.toSet()

            when {
                requiredStaticness.size == 2 -> holder.registerProblem(
                    identifier,
                    "Impossible combination of targets: some require a static handler and others a non-static handler",
                )

                true in requiredStaticness -> if (!method.hasModifierProperty(PsiModifier.STATIC)) {
                    holder.registerProblem(
                        identifier,
                        "Method must be static",
                        QuickFixFactory.getInstance().createModifierListFix(
                            modifiers,
                            PsiModifier.STATIC,
                            true,
                            false,
                        ),
                    )
                }

                false in requiredStaticness -> if (method.hasModifierProperty(PsiModifier.STATIC)) {
                    holder.registerProblem(
                        modifiers.findKeyword(PsiModifier.STATIC) ?: identifier,
                        "Method must not be static",
                        QuickFixFactory.getInstance().createModifierListFix(
                            modifiers,
                            PsiModifier.STATIC,
                            false,
                            false,
                        ),
                    )
                }
            }

            val isAlreadyValid = handler.expectedMethodSignatures(
                annotation,
                targetMethods,
                CollectVisitor.Mode.RESOLUTION,
            ).all { it.matches(method) }

            if (isAlreadyValid) {
                return
            }

            val signatureOptions =
                handler.expectedMethodSignatures(annotation, targetMethods).collectSignatures<MethodSignatures>()
            val signatureSuggestion = signatureOptions?.let { SuggestedReturnType.forParams(parameters, it) }
                ?: handler.suggestedMethodSignature(annotation, targetMethods)

            if (signatureSuggestion == null) {
                holder.registerProblem(
                    parameters,
                    "There are no possible signatures for this injector",
                )
            } else {
                val annotationName = annotation.nameReferenceElement?.referenceName
                val description =
                    "Method signature does not match expected signature for $annotationName"
                val quickFix = SignatureQuickFix(method, signatureSuggestion)
                val declarationStart = (method.returnTypeElement ?: identifier).startOffsetInParent
                val declarationEnd = method.parameterList.textRangeInParent.endOffset
                holder.registerProblem(
                    method,
                    description,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    TextRange.create(declarationStart, declarationEnd),
                    quickFix
                )
            }
        }
    }

    private class SignatureQuickFix(
        method: PsiMethod,
        @SafeFieldForPreview
        private val signatureSuggestion: SignatureSuggestion,
    ) : LocalQuickFixAndIntentionActionOnPsiElement(method) {

        private val fixName = "Fix method signature"

        override fun getFamilyName() = fixName

        override fun getText() = familyName

        override fun startInWriteAction() = false

        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement,
        ) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(startElement)) {
                return
            }
            val method = startElement as PsiMethod
            fixParameters(project, method.parameterList, false)
            fixReturnType(method, editor ?: return, file, false)
            fixCoerce(project, method, false)
            fixIntLikeTypes(project, method, editor, false)
        }

        override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
            val method = PsiTreeUtil.findSameElementInCopy(startElement, file) as? PsiMethod
                ?: return IntentionPreviewInfo.EMPTY
            fixParameters(project, method.parameterList, true)
            // Pass the original startElement because the underlying fix gets the preview element itself
            fixReturnType(startElement as PsiMethod, editor, file, true)
            fixCoerce(project, method, true)
            fixIntLikeTypes(project, method, editor, true)
            return IntentionPreviewInfo.DIFF
        }

        private fun fixParameters(project: Project, parameters: PsiParameterList, preview: Boolean) {
            val suggestedParams = signatureSuggestion.params ?: return
            // We want to preserve captured locals
            val locals = parameters.parameters.dropWhile {
                val fqname = (it.type as? PsiClassType)?.fullQualifiedName ?: return@dropWhile true
                return@dropWhile fqname != MixinConstants.Classes.CALLBACK_INFO &&
                    fqname != MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
            }.drop(1) // the first element in the list is the CallbackInfo but we don't want it
                .takeWhile { !it.isMixinExtrasSugar }

            // We want to preserve sugars, and while we're at it, we might as well move them all to the end
            val sugars = parameters.parameters.filter { it.isMixinExtrasSugar }

            val nameHelper = PsiNameHelper.getInstance(project)
            val languageLevel = PsiUtil.getLanguageLevel(parameters)

            val usedNames = mutableSetOf<String>()
            val newParams = suggestedParams.mapTo(mutableListOf()) { p ->
                val paramName = p.name?.takeIf { name -> nameHelper.isIdentifier(name, languageLevel) }
                    ?: suggestedParamNames(p.type).firstOrNull { it !in usedNames }
                    ?: VariableNameGenerator(parameters, VariableKind.PARAMETER)
                        .byType(p.type)
                        .skipNames(usedNames)
                        .generate(false)
                usedNames.add(paramName)
                val newParam = JavaPsiFacade.getElementFactory(project).createParameter(paramName, p.type)
                if (p.coerce) {
                    newParam.modifierList!!.addAnnotation(COERCE)
                }
                newParam
            }
            // Restore the captured locals and sugars before applying the fix
            newParams.addAll(locals)
            newParams.addAll(sugars)
            if (preview) {
                parameters.synchronize(newParams)
            } else {
                runWriteAction {
                    parameters.synchronize(newParams)
                }
            }
        }

        private fun fixReturnType(method: PsiMethod, editor: Editor, file: PsiFile, preview: Boolean) {
            val fix = QuickFixFactory.getInstance().createMethodReturnFix(method, signatureSuggestion.returnType, false)
            if (preview) {
                fix.generatePreview(file.project, editor, file)
            } else {
                fix.applyFix()
            }
        }

        private fun fixCoerce(project: Project, method: PsiMethod, preview: Boolean) {
            val existingCoerce = method.modifierList.findAnnotation(COERCE)
            val needsCoerce = signatureSuggestion.coerceReturnType
            val returnTypeElement = method.returnTypeElement!!

            val fixCoerce: () -> Unit = when {
                existingCoerce != null && !needsCoerce -> {
                    { existingCoerce.delete() }
                }

                existingCoerce == null && needsCoerce -> {
                    val annotation = JavaPsiFacade.getElementFactory(project)
                        .createAnnotationFromText("@$COERCE", returnTypeElement);
                    { returnTypeElement.addBefore(annotation, returnTypeElement.firstChild) }
                }

                else -> return
            }

            if (preview) {
                fixCoerce()
            } else {
                runWriteAction(fixCoerce)
            }
        }

        private fun fixIntLikeTypes(project: Project, method: PsiMethod, editor: Editor, preview: Boolean) {
            if (preview || signatureSuggestion.intLikeTypes.isEmpty()) {
                return
            }
            runWriteAction {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

                val template = makeIntLikeTypeTemplate(method, signatureSuggestion.intLikeTypes)
                if (template != null) {
                    editor.caretModel.moveToOffset(method.startOffset)
                    TemplateManager.getInstance(method.project)
                        .startTemplate(editor, template)
                }
            }
        }

        private fun makeIntLikeTypeTemplate(
            method: PsiMethod,
            positions: SequencedSet<MethodSignature.TypePosition>
        ): Template? {
            val builder = TemplateBuilderImpl(method)
            builder.replaceElement(
                positions.first().getElement(method) ?: return null,
                "intliketype",
                ChooseIntLikeTypeExpression(),
                true
            )
            for (pos in positions.drop(1)) {
                builder.replaceElement(
                    pos.getElement(method) ?: return null,
                    VariableNode("intliketype", null),
                    false
                )
            }
            return builder.buildInlineTemplate()
        }

        private companion object {
            fun suggestedParamNames(type: PsiType): List<String> = when {
                type.equalsToText(CALLBACK_INFO) -> listOf("ci")
                else -> emptyList()
            }
        }
    }
}

private class ChooseIntLikeTypeExpression : Expression() {
    private val lookupItems: Array<LookupElement> = intLikeTypes.map(LookupElementBuilder::create).toTypedArray()

    override fun calculateLookupItems(context: ExpressionContext) = if (lookupItems.size > 1) lookupItems else null

    override fun calculateQuickResult(context: ExpressionContext) = calculateResult(context)

    override fun calculateResult(context: ExpressionContext) = TextResult("int")

    private companion object {
        private val intLikeTypes = listOf(
            "int",
            "char",
            "boolean",
            "byte",
            "short"
        )
    }
}
