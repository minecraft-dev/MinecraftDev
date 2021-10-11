/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiType
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

        return listOf(MethodSignature(result, PsiType.VOID))
    }

    override val allowCoerce = true
}
