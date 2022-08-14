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

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AbstractLoadInjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.findModule
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyVariableHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature>? {
        val module = annotation.findModule() ?: return null

        val at = annotation.findAttributeValue("at") as? PsiAnnotation
        val atCode = at?.findAttributeValue("value")?.constantStringValue
        val isLoadStore = atCode != null && InjectionPoint.byAtCode(atCode) is AbstractLoadInjectionPoint
        val mode = if (isLoadStore) CollectVisitor.Mode.COMPLETION else CollectVisitor.Mode.MATCH_ALL
        val targets = resolveInstructions(annotation, targetClass, targetMethod, mode)

        val targetParamsGroup = ParameterGroup(
            collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
            required = ParameterGroup.RequiredLevel.OPTIONAL,
            isVarargs = true
        )

        val info = ModifyVariableInfo.getModifyVariableInfo(annotation, CollectVisitor.Mode.COMPLETION)
            ?: return null

        val possibleTypes = mutableSetOf<String>()
        for (insn in targets) {
            val locals = info.getLocals(module, targetClass, targetMethod, insn.insn) ?: continue
            val matchedLocals = info.matchLocals(locals, CollectVisitor.Mode.COMPLETION, matchType = false)
            for (local in matchedLocals) {
                possibleTypes += local.desc!!
            }
        }

        val result = mutableListOf<MethodSignature>()

        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
        for (type in possibleTypes) {
            val psiType = Type.getType(type).toPsiType(elementFactory)
            result += MethodSignature(
                listOf(
                    ParameterGroup(listOf(sanitizedParameter(psiType, "value"))),
                    targetParamsGroup
                ),
                psiType
            )
        }

        return result
    }
}

class ModifyVariableInfo(
    val type: PsiType?,
    val argsOnly: Boolean,
    val index: Int?,
    val ordinal: Int?,
    val names: Set<String>
) {
    fun getLocals(
        module: Module,
        targetClass: ClassNode,
        methodNode: MethodNode,
        insn: AbstractInsnNode
    ): Array<LocalVariables.LocalVariable?>? {
        return if (argsOnly) {
            val args = mutableListOf<LocalVariables.LocalVariable?>()
            if (!methodNode.hasAccess(Opcodes.ACC_STATIC)) {
                val thisDesc = Type.getObjectType(targetClass.name).descriptor
                args.add(LocalVariables.LocalVariable("this", thisDesc, null, null, null, 0))
            }
            for (argType in Type.getArgumentTypes(methodNode.desc)) {
                args.add(
                    LocalVariables.LocalVariable("arg${args.size}", argType.descriptor, null, null, null, args.size)
                )
                if (argType.size == 2) {
                    args.add(null)
                }
            }
            args.toTypedArray()
        } else {
            LocalVariables.getLocals(module, targetClass, methodNode, insn)
        }
    }

    fun matchLocals(
        locals: Array<LocalVariables.LocalVariable?>,
        mode: CollectVisitor.Mode,
        matchType: Boolean = true
    ): List<LocalVariables.LocalVariable> {
        val typeDesc = type?.descriptor
        if (ordinal != null) {
            val ordinals = mutableMapOf<String, Int>()
            val result = mutableListOf<LocalVariables.LocalVariable>()
            for (local in locals) {
                if (local == null) {
                    continue
                }
                val ordinal = ordinals[local.desc] ?: 0
                ordinals[local.desc!!] = ordinal + 1
                if (ordinal == this.ordinal && (!matchType || typeDesc == null || local.desc == typeDesc)) {
                    result += local
                }
            }
            return result
        }

        if (index != null) {
            val local = locals.firstOrNull { it?.index == index }
            if (local != null) {
                if (!matchType || typeDesc == null || local.desc == typeDesc) {
                    return listOf(local)
                }
            }
            return emptyList()
        }

        if (names.isNotEmpty()) {
            val result = mutableListOf<LocalVariables.LocalVariable>()
            for (local in locals) {
                if (local == null) {
                    continue
                }
                if (names.contains(local.name)) {
                    if (!matchType || typeDesc == null || local.desc == typeDesc) {
                        result += local
                    }
                }
            }
            return result
        }

        // implicit mode
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return locals.asSequence()
                .filterNotNull()
                .filter { local -> locals.count { it?.desc == local.desc } == 1 }
                .toList()
        }

        return if (matchType && typeDesc != null) {
            locals.singleOrNull { it?.desc == typeDesc }?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    companion object {
        fun getModifyVariableInfo(modifyVariable: PsiAnnotation, mode: CollectVisitor.Mode?): ModifyVariableInfo? {
            val method = modifyVariable.findContainingMethod() ?: return null
            val type = method.parameterList.getParameter(0)?.type
            if (type == null && mode != CollectVisitor.Mode.COMPLETION) {
                return null
            }
            val argsOnly = modifyVariable.findDeclaredAttributeValue("argsOnly")?.constantValue as? Boolean ?: false
            val index = (modifyVariable.findDeclaredAttributeValue("index")?.constantValue as? Int)
                ?.takeIf { it != -1 }
            val ordinal = (modifyVariable.findDeclaredAttributeValue("ordinal")?.constantValue as? Int)
                ?.takeIf { it != -1 }
            val names = modifyVariable.findDeclaredAttributeValue("name")?.computeStringArray()?.toSet() ?: emptySet()
            return ModifyVariableInfo(type, argsOnly, index, ordinal, names)
        }
    }
}
