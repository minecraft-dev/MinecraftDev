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
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.getParameter
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class ModifyArgHandler : InjectorAnnotationHandler() {
    override fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return insn is MethodInsnNode
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature>? {
        val index = annotation.findDeclaredAttributeValue("index")?.constantValue as? Int
        val validSingleArgTypes = mutableSetOf<String>()
        var mayHaveValidFullSignature = true
        var validFullSignature: String? = null
        val insns = resolveInstructions(annotation, targetClass, targetMethod).ifEmpty { return emptyList() }
        for (insn in insns) {
            if (insn.insn !is MethodInsnNode) return null

            // normalize return type so whole signature matches
            val desc = insn.insn.desc.replaceAfterLast(')', "V")

            if (index == null) {
                val validArgTypes = Type.getArgumentTypes(desc).mapTo(mutableListOf()) { it.descriptor }
                // remove duplicates completely, they are invalid
                val toRemove = validArgTypes.filter { e -> validArgTypes.count { it == e } > 1 }.toSet()
                validArgTypes.removeIf { toRemove.contains(it) }
                if (validArgTypes.isEmpty()) {
                    return listOf()
                }

                if (validSingleArgTypes.isEmpty()) {
                    validSingleArgTypes.addAll(validArgTypes)
                } else {
                    validSingleArgTypes.retainAll(validArgTypes)
                    if (validSingleArgTypes.isEmpty()) {
                        return listOf()
                    }
                }
            } else {
                val singleArgType = Type.getArgumentTypes(desc).getOrNull(index)?.descriptor ?: return listOf()
                if (validSingleArgTypes.isEmpty()) {
                    validSingleArgTypes += singleArgType
                } else {
                    validSingleArgTypes.removeIf { it != singleArgType }
                    if (validSingleArgTypes.isEmpty()) {
                        return listOf()
                    }
                }
            }

            if (mayHaveValidFullSignature) {
                if (validFullSignature == null) {
                    validFullSignature = desc
                } else {
                    if (desc != validFullSignature) {
                        validFullSignature = null
                        mayHaveValidFullSignature = false
                    }
                }
            }
        }

        // get the source method for parameter names
        val (bytecodeClass, bytecodeMethod) = (insns[0].insn as MethodInsnNode).fakeResolve()
        val sourceMethod = insns[0].target as? PsiMethod
        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
        return validSingleArgTypes.flatMap { type ->
            val paramList = sourceMethod?.parameterList
            val psiParameter = paramList?.parameters?.firstOrNull { it.type.descriptor == type }
            val psiType = psiParameter?.type ?: Type.getType(type).toPsiType(elementFactory, null)
            val singleSignature = MethodSignature(
                listOf(
                    ParameterGroup(
                        listOf(
                            sanitizedParameter(psiType, psiParameter?.name)
                        )
                    )
                ),
                psiType
            )
            if (validFullSignature != null) {
                val fullParamGroup = ParameterGroup(
                    Type.getArgumentTypes(validFullSignature).withIndex().map { (index, argType) ->
                        val psiParam = paramList?.let { bytecodeMethod.getParameter(bytecodeClass, index, it) }
                        sanitizedParameter(
                            psiParam?.type ?: argType.toPsiType(elementFactory),
                            psiParam?.name
                        )
                    }
                )
                listOf(
                    singleSignature,
                    MethodSignature(listOf(fullParamGroup), psiType)
                )
            } else {
                listOf(singleSignature)
            }
        }
    }
}
