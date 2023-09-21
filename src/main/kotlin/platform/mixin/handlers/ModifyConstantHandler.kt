/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.ConstantInjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.util.findAnnotations
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyConstantHandler : InjectorAnnotationHandler() {
    private val allowedOpcodes = setOf(
        Opcodes.ICONST_M1,
        Opcodes.ICONST_0,
        Opcodes.ICONST_1,
        Opcodes.ICONST_2,
        Opcodes.ICONST_3,
        Opcodes.ICONST_4,
        Opcodes.ICONST_5,
        Opcodes.LCONST_0,
        Opcodes.LCONST_1,
        Opcodes.FCONST_0,
        Opcodes.FCONST_1,
        Opcodes.FCONST_2,
        Opcodes.DCONST_0,
        Opcodes.DCONST_1,
        Opcodes.BIPUSH,
        Opcodes.SIPUSH,
        Opcodes.LDC,
        Opcodes.IFLT,
        Opcodes.IFGE,
        Opcodes.IFGT,
        Opcodes.IFLE,
    )

    private fun getConstantInfos(modifyConstant: PsiAnnotation): List<ConstantInjectionPoint.ConstantInfo>? {
        val constants = modifyConstant.findDeclaredAttributeValue("constant")
            ?.findAnnotations()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return constants.map { constant ->
            (InjectionPoint.byAtCode("CONSTANT") as ConstantInjectionPoint).getConstantInfo(constant) ?: return null
        }
    }

    override fun getAtKey(annotation: PsiAnnotation) = "constant"

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): List<MethodSignature>? {
        val constantInfos = getConstantInfos(annotation)
        if (constantInfos == null) {
            val method = annotation.parentOfType<PsiMethod>()
                ?: return emptyList()
            val returnType = method.returnType
                ?: return emptyList()
            val constantParamName = method.parameterList.getParameter(0)?.name ?: "constant"
            return listOf(
                MethodSignature(
                    listOf(
                        ParameterGroup(listOf(sanitizedParameter(returnType, constantParamName))),
                        ParameterGroup(
                            collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                            isVarargs = true,
                            required = ParameterGroup.RequiredLevel.OPTIONAL,
                        ),
                    ),
                    returnType,
                )
            )
        }

        val psiManager = PsiManager.getInstance(annotation.project)
        return constantInfos.asSequence().map {
            when (it.constant) {
                null -> PsiType.getJavaLangObject(psiManager, annotation.resolveScope)
                is Int -> PsiType.INT
                is Float -> PsiType.FLOAT
                is Long -> PsiType.LONG
                is Double -> PsiType.DOUBLE
                is String -> PsiType.getJavaLangString(psiManager, annotation.resolveScope)
                is Type -> PsiType.getJavaLangClass(psiManager, annotation.resolveScope)
                else -> throw IllegalStateException("Unknown constant type: ${it.constant.javaClass.name}")
            }
        }.distinct().map { type ->
            MethodSignature(
                listOf(
                    ParameterGroup(listOf(sanitizedParameter(type, "constant"))),
                    ParameterGroup(
                        collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                        isVarargs = true,
                        required = ParameterGroup.RequiredLevel.OPTIONAL,
                    ),
                ),
                type,
            )
        }.toList()
    }

    override fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return insn.opcode in allowedOpcodes
    }
}
