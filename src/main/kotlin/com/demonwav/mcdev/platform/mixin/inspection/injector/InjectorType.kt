/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier.STATIC
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiType
import org.jetbrains.org.objectweb.asm.Opcodes

enum class InjectorType(private val annotation: String) {
    INJECT(MixinConstants.Annotations.INJECT) {

        override fun expectedMethodSignature(annotation: PsiAnnotation, targetMethod: PsiMethod): MethodSignature? {
            val returnType = targetMethod.returnType

            val result = ArrayList<ParameterGroup>()

            // Parameters from injected method (optional)
            result.add(ParameterGroup(collectTargetMethodParameters(targetMethod), required = false, default = true))

            // Callback info (required)
            result.add(
                ParameterGroup(
                    listOf(
                        if (returnType == null || returnType == PsiType.VOID) {
                            Parameter("ci", callbackInfoType(targetMethod.project)!!)
                        } else {
                            Parameter(
                                "cir",
                                callbackInfoReturnableType(targetMethod.project, targetMethod, returnType)!!
                            )
                        }
                    )
                )
            )

            // Captured locals (only if local capture is enabled)
            // Right now we allow any parameters here since we can't easily
            // detect the local variables that can be captured
            if (((annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)
                    ?.referenceName ?: "NO_CAPTURE") != "NO_CAPTURE"
            ) {
                result.add(ParameterGroup(null))
            }

            return MethodSignature(result, PsiType.VOID)
        }
    },
    REDIRECT(MixinConstants.Annotations.REDIRECT) {

        override fun expectedMethodSignature(annotation: PsiAnnotation, targetMethod: PsiMethod): MethodSignature? {
            val at = annotation.findDeclaredAttributeValue("at") as? PsiAnnotation ?: return null
            val target = at.findDeclaredAttributeValue("target") ?: return null

            if (!TargetReference.usesMemberReference(target)) {
                return null
            }

            // Since the target reference is required to be full qualified,
            // we don't actually have to resolve the target reference in the
            // target method. Everything needed to get the method parameters
            // is included in the reference.
            val reference = MixinMemberReference.parse(target.constantStringValue) ?: return null

            if (!reference.qualified || reference.descriptor == null) {
                // Invalid anyway and we need the qualified reference
                return null
            }

            val (owner, member) = reference.resolve(annotation.project, annotation.resolveScope) ?: return null
            val (parameters, returnType) = when (member) {
                is PsiMethod -> collectMethodParameters(owner, member)
                is PsiField -> collectFieldParameters(at, owner, member)
                else -> throw AssertionError("Cannot resolve member reference to: $member")
            } ?: return null

            val primaryGroup = ParameterGroup(parameters, required = true)

            // Optionally the target method parameters can be used
            val targetMethodGroup = ParameterGroup(collectTargetMethodParameters(targetMethod), required = false)

            return MethodSignature(listOf(primaryGroup, targetMethodGroup), returnType)
        }

        private fun getInstanceParameter(owner: PsiClass): Parameter {
            return Parameter(null, JavaPsiFacade.getElementFactory(owner.project).createType(owner))
        }

        private fun collectMethodParameters(owner: PsiClass, method: PsiMethod): Pair<List<Parameter>, PsiType>? {
            val parameterList = method.parameterList
            val parameters = ArrayList<Parameter>(parameterList.parametersCount + 1)

            val returnType = if (method.isConstructor) {
                PsiType.VOID
            } else {
                if (!method.hasModifierProperty(STATIC)) {
                    parameters.add(getInstanceParameter(owner))
                }

                method.returnType!!
            }

            parameterList.parameters.mapTo(parameters, ::Parameter)
            return Pair(parameters, returnType)
        }

        private fun collectFieldParameters(
            at: PsiAnnotation,
            owner: PsiClass,
            field: PsiField
        ): Pair<List<Parameter>, PsiType>? {
            // TODO: Report if opcode isn't set
            val opcode = at.findDeclaredAttributeValue("opcode")?.constantValue as? Int ?: return null

            // TODO: Report if magic value is used instead of a reference to a field (e.g. to ASM's Opcodes interface)
            // TODO: Report if opcode is invalid (not one of GETSTATIC, GETFIELD, PUTSTATIC, PUTFIELD)

            val parameters = ArrayList<Parameter>(2)

            // TODO: Report if GETSTATIC/PUTSTATIC is used for an instance field
            if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                parameters.add(getInstanceParameter(owner))
            }

            val returnType = when (opcode) {
                Opcodes.GETFIELD, Opcodes.GETSTATIC -> field.type
                Opcodes.PUTFIELD, Opcodes.PUTSTATIC -> {
                    parameters.add(Parameter("value", field.type))
                    PsiType.VOID
                }
                else -> return null // Invalid opcode
            }

            return Pair(parameters, returnType)
        }
    },
    MODIFY_ARG(MixinConstants.Annotations.MODIFY_ARG),
    MODIFY_CONSTANT(MixinConstants.Annotations.MODIFY_CONSTANT),
    MODIFY_VARIABLE(MixinConstants.Annotations.MODIFY_VARIABLE);

    val annotationName = "@${PsiNameHelper.getShortClassName(annotation)}"

    open fun expectedMethodSignature(annotation: PsiAnnotation, targetMethod: PsiMethod): MethodSignature? = null

    companion object {

        private val injectionPointAnnotations = InjectorType.values().associateBy { it.annotation }

        private fun collectTargetMethodParameters(targetMethod: PsiMethod): List<Parameter> {
            val parameters = targetMethod.parameterList.parameters
            val list = ArrayList<Parameter>(parameters.size)

            // Special handling for enums: When compiled, the Java compiler
            // prepends the name and the ordinal to the constructor
            if (targetMethod.isConstructor) {
                val containingClass = targetMethod.containingClass
                if (containingClass != null && containingClass.isEnum) {
                    list.add(
                        Parameter(
                            "enumName",
                            PsiType.getJavaLangString(targetMethod.manager, targetMethod.resolveScope)
                        )
                    )
                    list.add(Parameter("ordinal", PsiType.INT))
                }
            }

            // Add method parameters to list
            parameters.mapTo(list, ::Parameter)
            return list
        }

        fun findAnnotations(element: PsiAnnotationOwner): List<Pair<InjectorType, PsiAnnotation>> {
            return element.annotations.mapNotNull {
                val name = it.qualifiedName ?: return@mapNotNull null
                val type = injectionPointAnnotations[name] ?: return@mapNotNull null
                Pair(type, it)
            }
        }
    }
}
