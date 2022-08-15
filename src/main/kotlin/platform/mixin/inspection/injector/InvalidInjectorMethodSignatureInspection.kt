/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InjectAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.synchronize
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.TypeConversionUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode

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
                val qName = annotation.qualifiedName ?: continue
                val handler = MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project)
                    as? InjectorAnnotationHandler ?: continue
                val methodAttribute = annotation.findDeclaredAttributeValue("method") ?: continue
                val targetMethods = MethodReference.resolveAllIfNotAmbiguous(methodAttribute) ?: continue

                for (targetMethod in targetMethods) {
                    if (!reportedStatic) {
                        var shouldBeStatic = targetMethod.method.hasAccess(Opcodes.ACC_STATIC)

                        if (!shouldBeStatic && targetMethod.method.isConstructor) {
                            // before the superclass constructor call, everything must be static
                            targetMethod.method.instructions?.let { methodInsns ->
                                val superCtorCall = findSuperConstructorCall(methodInsns)
                                val insns = handler.resolveInstructions(
                                    annotation,
                                    targetMethod.clazz,
                                    targetMethod.method
                                )
                                shouldBeStatic = insns.any {
                                    methodInsns.indexOf(it.insn) <= methodInsns.indexOf(superCtorCall)
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
                                    false
                                )
                            )
                        }
                    }

                    if (!reportedSignature) {
                        // Check method parameters
                        val parameters = method.parameterList
                        val possibleSignatures = handler.expectedMethodSignature(
                            annotation,
                            targetMethod.clazz,
                            targetMethod.method
                        ) ?: continue

                        val annotationName = annotation.nameReferenceElement?.referenceName

                        if (possibleSignatures.isEmpty()) {
                            reportedSignature = true
                            holder.registerProblem(
                                parameters,
                                "There are no possible signatures for this injector"
                            )
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
                            val (expectedParameters, expectedReturnType) = possibleSignatures[0]

                            val checkResult = checkParameters(parameters, expectedParameters, handler.allowCoerce)
                            if (checkResult != CheckResult.OK) {
                                reportedSignature = true

                                val description =
                                    "Method parameters do not match expected parameters for $annotationName"
                                val quickFix = ParametersQuickFix(
                                    expectedParameters,
                                    handler is InjectAnnotationHandler
                                )
                                if (checkResult == CheckResult.ERROR) {
                                    holder.registerProblem(parameters, description, quickFix)
                                } else {
                                    holder.registerProblem(
                                        parameters,
                                        description,
                                        ProblemHighlightType.WARNING,
                                        quickFix
                                    )
                                }
                            }

                            val methodReturnType = method.returnType
                            if (methodReturnType == null ||
                                !checkReturnType(expectedReturnType, methodReturnType, method, handler.allowCoerce)
                            ) {
                                reportedSignature = true

                                holder.registerProblem(
                                    method.returnTypeElement ?: identifier,
                                    "Expected return type '${expectedReturnType.presentableText}' " +
                                        "for $annotationName method",
                                    QuickFixFactory.getInstance().createMethodReturnFix(
                                        method,
                                        expectedReturnType,
                                        false
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun findSuperConstructorCall(methodInsns: InsnList): AbstractInsnNode? {
            var superCtorCall = methodInsns.first
            var newCount = 0
            while (superCtorCall != null) {
                if (superCtorCall.opcode == Opcodes.NEW) {
                    newCount++
                } else if (superCtorCall.opcode == Opcodes.INVOKESPECIAL) {
                    val methodCall = superCtorCall as MethodInsnNode
                    if (methodCall.name == "<init>") {
                        if (newCount == 0) {
                            return superCtorCall
                        } else {
                            newCount--
                        }
                    }
                }
                superCtorCall = superCtorCall.next
            }
            return null
        }

        private fun checkReturnType(
            expectedReturnType: PsiType,
            methodReturnType: PsiType,
            method: PsiMethod,
            allowCoerce: Boolean
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
            allowCoerce: Boolean
        ): CheckResult {
            val parameters = parameterList.parameters
            var pos = 0

            for (group in expected) {
                // Check if parameter group matches
                if (group.match(parameters, pos, allowCoerce)) {
                    pos += group.size
                } else if (group.required != ParameterGroup.RequiredLevel.OPTIONAL) {
                    return if (group.required == ParameterGroup.RequiredLevel.ERROR_IF_ABSENT) {
                        CheckResult.ERROR
                    } else {
                        CheckResult.WARNING
                    }
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

    private class ParametersQuickFix(
        private val expected: List<ParameterGroup>,
        isInject: Boolean
    ) : LocalQuickFix {

        private val fixName = if (isInject) {
            "Fix method parameters"
        } else {
            "Fix method parameters (won't keep captured locals)"
        }

        override fun getFamilyName() = fixName

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val parameters = descriptor.psiElement as PsiParameterList
            // We want to preserve captured locals
            val locals = parameters.parameters.dropWhile {
                val fqname = (it.type as? PsiClassType)?.fullQualifiedName ?: return@dropWhile true
                return@dropWhile fqname != MixinConstants.Classes.CALLBACK_INFO &&
                    fqname != MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
            }.drop(1) // the first element in the list is the CallbackInfo but we don't want it
            val newParams = expected.flatMapTo(mutableListOf()) {
                if (it.default) {
                    it.parameters.mapIndexed { i: Int, p: Parameter ->
                        JavaPsiFacade.getElementFactory(project).createParameter(
                            p.name ?: JavaCodeStyleManager.getInstance(project)
                                .suggestVariableName(VariableKind.PARAMETER, null, null, p.type).names
                                .firstOrNull() ?: "var$i",
                            p.type
                        )
                    }
                } else {
                    emptyList()
                }
            }
            // Restore the captured locals before applying the fix
            newParams.addAll(locals)
            parameters.synchronize(newParams)
        }
    }
}
