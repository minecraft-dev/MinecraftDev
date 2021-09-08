/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.argsType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.platform.mixin.util.getGenericParameterTypes
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.platform.mixin.util.getGenericType
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

enum class InjectorType(private val annotation: String) {
    INJECT(MixinConstants.Annotations.INJECT) {

        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode
        ): MethodSignature {
            val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)

            val result = ArrayList<ParameterGroup>()

            // Parameters from injected method (optional)
            result.add(
                ParameterGroup(
                    collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                    required = false,
                    default = true
                )
            )

            // Callback info (required)
            result.add(
                ParameterGroup(
                    listOf(
                        if (returnType == PsiType.VOID) {
                            Parameter("ci", callbackInfoType(annotation.project))
                        } else {
                            Parameter(
                                "cir",
                                callbackInfoReturnableType(annotation.project, annotation, returnType)!!
                            )
                        }
                    )
                )
            )

            // Captured locals (only if local capture is enabled)
            // Right now we allow any parameters here since we can't easily
            // detect the local variables that can be captured
            // TODO: now we can work with the bytecode, revisit this?
            if ((
                (annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)
                    ?.referenceName ?: "NO_CAPTURE"
                ) != "NO_CAPTURE"
            ) {
                result.add(ParameterGroup(null))
            }

            return MethodSignature(result, PsiType.VOID)
        }
    },
    REDIRECT(MixinConstants.Annotations.REDIRECT) {

        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode
        ): MethodSignature? {
            val at = annotation.findDeclaredAttributeValue("at") as? PsiAnnotation ?: return null
            val target = at.findDeclaredAttributeValue("target") ?: return null

            if (!TargetReference.usesMemberReference(target)) {
                return null
            }

            // Since the target reference is required to be full qualified,
            // we don't actually have to resolve the target reference in the
            // target method. Everything needed to get the method parameters
            // is included in the reference.
            val selector = parseMixinSelector(target) ?: return null

            if (!selector.qualified || (selector.methodDescriptor == null && selector.fieldDescriptor == null)) {
                // Invalid anyway and we need the qualified reference
                return null
            }

            val member = selector.resolveAsm(annotation.project, annotation.resolveScope) ?: return null
            val (parameters, returnType) = when (member) {
                is MethodTargetMember -> collectMethodParameters(
                    annotation.project,
                    selector,
                    member.classAndMethod.clazz,
                    member.classAndMethod.method
                )
                is FieldTargetMember -> collectFieldParameters(
                    annotation.project,
                    at,
                    selector,
                    member.classAndField.clazz,
                    member.classAndField.field
                )
            } ?: return null

            val primaryGroup = ParameterGroup(parameters, required = true)

            // Optionally the target method parameters can be used
            val targetMethodGroup = ParameterGroup(
                collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                required = false
            )

            return MethodSignature(listOf(primaryGroup, targetMethodGroup), returnType)
        }

        private fun collectMethodParameters(
            project: Project,
            selector: MixinSelector,
            clazz: ClassNode,
            method: MethodNode
        ): Pair<List<Parameter>, PsiType>? {
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val ownerName = selector.owner ?: return null

            val hasThis = !method.hasAccess(Opcodes.ACC_STATIC)
            val parameters = mutableListOf<Parameter>()
            if (hasThis) {
                parameters += Parameter(
                    "self",
                    Type.getObjectType(ownerName.replace('.', '/')).toPsiType(elementFactory)
                )
            }

            method.getGenericParameterTypes(clazz, project)
                .asSequence()
                .withIndex()
                .mapTo(parameters) { (index, type) ->
                    val i = if (hasThis) index + 1 else index
                    val name = method.localVariables?.getOrNull(i)?.name?.toJavaIdentifier() ?: "par${index + 1}"
                    sanitizedParameter(type, name)
                }

            val returnType = method.getGenericReturnType(clazz, project)
            return parameters to returnType
        }

        private fun collectFieldParameters(
            project: Project,
            at: PsiAnnotation,
            selector: MixinSelector,
            clazz: ClassNode,
            field: FieldNode
        ): Pair<List<Parameter>, PsiType>? {
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val ownerName = selector.owner ?: return null

            // TODO: Report if opcode isn't set
            val opcode = at.findDeclaredAttributeValue("opcode")?.constantValue as? Int ?: return null

            // TODO: Report if magic value is used instead of a reference to a field (e.g. to ASM's Opcodes interface)
            // TODO: Report if opcode is invalid (not one of GETSTATIC, GETFIELD, PUTSTATIC, PUTFIELD)

            val parameters = ArrayList<Parameter>(2)

            // TODO: Report if GETSTATIC/PUTSTATIC is used for an instance field
            if (!field.hasAccess(Opcodes.ACC_STATIC)) {
                parameters += Parameter(
                    "self",
                    Type.getObjectType(ownerName.replace('.', '/')).toPsiType(elementFactory)
                )
            }

            val returnType = when (opcode) {
                Opcodes.PUTFIELD, Opcodes.PUTSTATIC -> {
                    parameters.add(Parameter("value", field.getGenericType(clazz, project)))
                    PsiType.VOID
                }
                else -> { // assume getfield redirect
                    field.getGenericType(clazz, project)
                }
            }

            return Pair(parameters, returnType)
        }
    },
    MODIFY_ARG(MixinConstants.Annotations.MODIFY_ARG),
    MODIFY_ARGS(MixinConstants.Annotations.MODIFY_ARGS) {
        override fun expectedMethodSignature(
            annotation: PsiAnnotation,
            targetClass: ClassNode,
            targetMethod: MethodNode
        ): MethodSignature {
            val result = ArrayList<ParameterGroup>()

            // Args object (required)
            result.add(ParameterGroup(listOf(Parameter("args", argsType(annotation.project)))))

            // Parameters from injected method (optional)
            result.add(
                ParameterGroup(
                    collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                    required = false
                )
            )

            return MethodSignature(result, PsiType.VOID)
        }
    },
    MODIFY_CONSTANT(MixinConstants.Annotations.MODIFY_CONSTANT),
    MODIFY_VARIABLE(MixinConstants.Annotations.MODIFY_VARIABLE);

    val annotationName = "@${PsiNameHelper.getShortClassName(annotation)}"

    open fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): MethodSignature? = null

    companion object {

        private val injectionPointAnnotations = InjectorType.values().associateBy { it.annotation }

        private fun collectTargetMethodParameters(
            project: Project,
            clazz: ClassNode,
            targetMethod: MethodNode
        ): List<Parameter> {
            val numLocalsToDrop = if (targetMethod.hasAccess(Opcodes.ACC_STATIC)) 0 else 1
            return targetMethod.getGenericParameterTypes(clazz, project).asSequence().withIndex()
                .map { (index, type) ->
                    val name = targetMethod.localVariables
                        ?.getOrNull(index + numLocalsToDrop)
                        ?.name
                        ?.toJavaIdentifier()
                        ?: "par${index + 1}"
                    type to name
                }
                .map { (type, name) -> sanitizedParameter(type, name) }
                .toList()
        }

        private fun sanitizedParameter(type: PsiType, name: String): Parameter {
            // Parameters should not use ellipsis because others like CallbackInfo may follow
            return if (type is PsiEllipsisType) {
                Parameter(name, type.toArrayType())
            } else {
                Parameter(name, type)
            }
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
