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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.platform.mixin.util.findDelegateConstructorCall
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.platform.mixin.util.isMixinExtrasSugar
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.findKeyword
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.invokeLater
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
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.startOffset
import org.objectweb.asm.Opcodes

class InvalidInjectorMethodSignatureInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports problems related to the method signature of Mixin injectors"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            val identifier = method.nameIdentifier ?: return
            val modifiers = method.modifierList

            var reportedStatic = false
            var reportedSignature = false

            for (annotation in modifiers.annotations) {
                val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, annotation.project)
                    as? InjectorAnnotationHandler ?: continue
                val methodAttribute = annotation.findDeclaredAttributeValue("method") ?: continue
                val targetMethods = MethodReference.resolveAllIfNotAmbiguous(methodAttribute) ?: continue

                for (targetMethod in targetMethods) {
                    if (!reportedStatic) {
                        var shouldBeStatic = targetMethod.method.hasAccess(Opcodes.ACC_STATIC)

                        if (!shouldBeStatic && targetMethod.method.isConstructor) {
                            // before the superclass constructor call, everything must be static
                            val methodInsns = targetMethod.method.instructions
                            val delegateCtorCall = targetMethod.method.findDelegateConstructorCall()
                            if (methodInsns != null && delegateCtorCall != null) {
                                val insns = handler.resolveInstructions(
                                    annotation,
                                    targetMethod.clazz,
                                    targetMethod.method,
                                )
                                shouldBeStatic = insns.any {
                                    methodInsns.indexOf(it.insn) <= methodInsns.indexOf(delegateCtorCall)
                                }
                            }
                        }

                        if (shouldBeStatic && !modifiers.hasModifierProperty(PsiModifier.STATIC)) {
                            reportedStatic = true
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
                        } else if (!shouldBeStatic && modifiers.hasModifierProperty(PsiModifier.STATIC)) {
                            reportedStatic = true
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

                    if (!reportedSignature) {
                        // Check method parameters
                        val parameters = method.parameterList
                        val possibleSignatures = handler.expectedMethodSignature(
                            annotation,
                            targetMethod.clazz,
                            targetMethod.method,
                        ) ?: continue

                        val annotationName = annotation.nameReferenceElement?.referenceName

                        if (possibleSignatures.isEmpty()) {
                            reportedSignature = true
                            if (handler.isUnresolved(annotation) != null) {
                                holder.registerProblem(
                                    parameters,
                                    "There are no possible signatures for this injector",
                                )
                            }
                            continue
                        }

                        var isValid = false
                        for ((expectedParameters, expectedReturnType) in possibleSignatures) {
                            val paramsMatch =
                                checkParameters(parameters, expectedParameters, handler.allowCoerce) == CheckResult.OK
                            if (paramsMatch) {
                                val methodReturnType = method.returnType
                                if (methodReturnType != null &&
                                    checkReturnType(expectedReturnType, methodReturnType, method, handler.allowCoerce)
                                ) {
                                    isValid = true
                                    break
                                }
                            }
                        }

                        if (!isValid) {
                            val (expectedParameters, expectedReturnType, intLikeTypePositions) = possibleSignatures[0]
                            val normalizedReturnType = when (expectedReturnType) {
                                is PsiEllipsisType -> expectedReturnType.toArrayType()
                                else -> expectedReturnType
                            }

                            val paramsCheck = checkParameters(parameters, expectedParameters, handler.allowCoerce)
                            val isWarning = paramsCheck == CheckResult.WARNING
                            val methodReturnType = method.returnType
                            val returnTypeOk = methodReturnType != null &&
                                checkReturnType(normalizedReturnType, methodReturnType, method, handler.allowCoerce)
                            val isError = paramsCheck == CheckResult.ERROR || !returnTypeOk
                            if (isWarning || isError) {
                                reportedSignature = true

                                val description =
                                    "Method signature does not match expected signature for $annotationName"
                                val quickFix = SignatureQuickFix(
                                    method,
                                    expectedParameters.takeUnless { paramsCheck == CheckResult.OK },
                                    normalizedReturnType.takeUnless { returnTypeOk },
                                    intLikeTypePositions
                                )
                                val highlightType =
                                    if (isError)
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                                    else
                                        ProblemHighlightType.WARNING
                                val declarationStart = (method.returnTypeElement ?: identifier).startOffsetInParent
                                val declarationEnd = method.parameterList.textRangeInParent.endOffset
                                holder.registerProblem(
                                    method,
                                    description,
                                    highlightType,
                                    TextRange.create(declarationStart, declarationEnd),
                                    quickFix
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun checkReturnType(
            expectedReturnType: PsiType,
            methodReturnType: PsiType,
            method: PsiMethod,
            allowCoerce: Boolean,
        ): Boolean {
            val expectedErasure = TypeConversionUtil.erasure(expectedReturnType)
            val returnErasure = TypeConversionUtil.erasure(methodReturnType)
            if (expectedErasure == returnErasure) {
                return true
            }
            if (!allowCoerce || !method.hasAnnotation(COERCE)) {
                return false
            }
            if (expectedReturnType is PsiPrimitiveType || methodReturnType is PsiPrimitiveType) {
                return false
            }
            return isAssignable(expectedReturnType, methodReturnType)
        }

        private fun checkParameters(
            parameterList: PsiParameterList,
            expected: List<ParameterGroup>,
            allowCoerce: Boolean,
        ): CheckResult {
            val parameters = parameterList.parameters
            val parametersWithoutSugar = parameters.dropLastWhile { it.isMixinExtrasSugar }.toTypedArray()
            var pos = 0

            for (group in expected) {
                // Check if parameter group matches
                if (group.match(parametersWithoutSugar, pos, allowCoerce)) {
                    pos += group.size
                } else if (group.required != ParameterGroup.RequiredLevel.OPTIONAL) {
                    return if (group.required == ParameterGroup.RequiredLevel.ERROR_IF_ABSENT) {
                        CheckResult.ERROR
                    } else {
                        CheckResult.WARNING
                    }
                }
            }

            // Sugars are valid on any injector and should be ignored, as long as they're at the end.
            while (pos < parameters.size) {
                if (parameters[pos].isMixinExtrasSugar) {
                    pos++
                } else {
                    break
                }
            }

            // check we have consumed all the parameters
            if (pos < parameters.size) {
                return if (
                    expected.lastOrNull()?.isVarargs == true &&
                    expected.last().required == ParameterGroup.RequiredLevel.WARN_IF_ABSENT
                ) {
                    CheckResult.WARNING
                } else {
                    CheckResult.ERROR
                }
            }

            return CheckResult.OK
        }
    }

    private enum class CheckResult {
        OK, WARNING, ERROR
    }

    private class SignatureQuickFix(
        method: PsiMethod,
        @SafeFieldForPreview
        private val expectedParams: List<ParameterGroup>?,
        @SafeFieldForPreview
        private val expectedReturnType: PsiType?,
        private val intLikeTypePositions: List<MethodSignature.TypePosition>
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
            fixIntLikeTypes(method, editor, false)
        }

        override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
            val method = PsiTreeUtil.findSameElementInCopy(startElement, file) as? PsiMethod
                ?: return IntentionPreviewInfo.EMPTY
            fixParameters(project, method.parameterList, true)
            // Pass the original startElement because the underlying fix gets the preview element itself
            fixReturnType(startElement as PsiMethod, editor, file, true)
            fixIntLikeTypes(method, editor, true)
            return IntentionPreviewInfo.DIFF
        }

        private fun fixParameters(project: Project, parameters: PsiParameterList, preview: Boolean) {
            if (expectedParams == null) {
                return
            }
            // We want to preserve captured locals
            val locals = parameters.parameters.dropWhile {
                val fqname = (it.type as? PsiClassType)?.fullQualifiedName ?: return@dropWhile true
                return@dropWhile fqname != MixinConstants.Classes.CALLBACK_INFO &&
                    fqname != MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
            }.drop(1) // the first element in the list is the CallbackInfo but we don't want it
                .takeWhile { !it.isMixinExtrasSugar }

            // We want to preserve sugars, and while we're at it, we might as well move them all to the end
            val sugars = parameters.parameters.filter { it.isMixinExtrasSugar }

            val newParams = expectedParams.flatMapTo(mutableListOf()) {
                if (it.default) {
                    val nameHelper = PsiNameHelper.getInstance(project)
                    val languageLevel = PsiUtil.getLanguageLevel(parameters)
                    it.parameters.mapIndexed { i: Int, p: Parameter ->
                        val paramName = p.name?.takeIf { name -> nameHelper.isIdentifier(name, languageLevel) }
                            ?: JavaCodeStyleManager.getInstance(project)
                                .suggestVariableName(VariableKind.PARAMETER, null, null, p.type).names
                                .firstOrNull()
                            ?: "var$i"
                        JavaPsiFacade.getElementFactory(project).createParameter(paramName, p.type)
                    }
                } else {
                    emptyList()
                }
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
            if (expectedReturnType == null) {
                return
            }
            val fix = QuickFixFactory.getInstance().createMethodReturnFix(method, expectedReturnType, false)
            if (preview) {
                fix.generatePreview(file.project, editor, file)
            } else {
                fix.applyFix()
            }
        }

        private fun fixIntLikeTypes(method: PsiMethod, editor: Editor, preview: Boolean) {
            if (intLikeTypePositions.isEmpty()) {
                return
            }
            val runnable = {
                val template = makeIntLikeTypeTemplate(method, intLikeTypePositions)
                if (template != null) {
                    editor.caretModel.moveToOffset(method.startOffset)
                    TemplateManager.getInstance(method.project)
                        .startTemplate(editor, template)
                }
            }

            if (preview) {
                runnable()
            } else {
                invokeLater {
                    WriteCommandAction.runWriteCommandAction(
                        method.project,
                        "Choose Int-Like Type",
                        null,
                        runnable,
                        method.parentOfType<PsiFile>()!!
                    )
                }
            }
        }

        private fun makeIntLikeTypeTemplate(
            method: PsiMethod,
            positions: List<MethodSignature.TypePosition>
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
