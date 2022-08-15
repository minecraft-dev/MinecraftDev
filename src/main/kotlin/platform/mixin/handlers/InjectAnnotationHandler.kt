/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.firstIndexOrNull
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiType
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class InjectAnnotationHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature> {
        val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)

        val result = ArrayList<ParameterGroup>()

        // Parameters from injected method (optional)
        result.add(
            ParameterGroup(
                collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                required = ParameterGroup.RequiredLevel.OPTIONAL,
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
        val localCapture = (annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)
            ?.referenceName ?: "NO_CAPTURE"
        if (localCapture != "NO_CAPTURE") {
            annotation.findModule()?.let { module ->
                var commonLocalsPrefix: MutableList<LocalVariables.LocalVariable>? = null
                val resolvedInsns = resolveInstructions(annotation, targetClass, targetMethod).ifEmpty { return@let }
                for (insn in resolvedInsns) {
                    val locals = LocalVariables.getLocals(module, targetClass, targetMethod, insn.insn)
                        ?.drop(
                            Type.getArgumentTypes(targetMethod.desc).size +
                                if (targetMethod.hasAccess(Opcodes.ACC_STATIC)) 0 else 1
                        )
                        ?.filterNotNull()
                        ?.filter { it.desc != null }
                        ?: continue
                    if (commonLocalsPrefix == null) {
                        commonLocalsPrefix = locals.toMutableList()
                    } else {
                        val mismatch = commonLocalsPrefix.zip(locals).firstIndexOrNull { (a, b) -> a.desc != b.desc }
                        if (mismatch != null) {
                            commonLocalsPrefix.subList(mismatch, commonLocalsPrefix.size).clear()
                        }
                    }
                }

                if (commonLocalsPrefix != null) {
                    val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
                    val localParams = commonLocalsPrefix.map { local ->
                        val type =
                            Type.getType(local.desc).toPsiType(elementFactory, annotation.parentOfType<PsiMethod>())
                        sanitizedParameter(type, local.name)
                    }
                    val requiredLevel = if (localCapture == "CAPTURE_FAILSOFT") {
                        ParameterGroup.RequiredLevel.WARN_IF_ABSENT
                    } else {
                        ParameterGroup.RequiredLevel.ERROR_IF_ABSENT
                    }
                    result.add(
                        ParameterGroup(
                            localParams,
                            default = true,
                            required = requiredLevel,
                            isVarargs = true
                        )
                    )
                }
            }
        }

        return listOf(MethodSignature(result, PsiType.VOID))
    }

    override val allowCoerce = true
}
