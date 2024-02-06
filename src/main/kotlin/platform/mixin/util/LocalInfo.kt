/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import com.intellij.util.containers.sequenceOfNotNull
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class LocalInfo(
    val type: PsiType?,
    val argsOnly: Boolean,
    val index: Int?,
    val ordinal: Int?,
    val names: Set<String>,
) {
    fun getLocals(
        module: Module,
        targetClass: ClassNode,
        methodNode: MethodNode,
        insn: AbstractInsnNode,
    ): Array<LocalVariables.LocalVariable?>? {
        return if (argsOnly) {
            val args = mutableListOf<LocalVariables.LocalVariable?>()
            if (!methodNode.hasAccess(Opcodes.ACC_STATIC)) {
                val thisDesc = Type.getObjectType(targetClass.name).descriptor
                args.add(LocalVariables.LocalVariable("this", thisDesc, null, null, null, 0))
            }
            for (argType in Type.getArgumentTypes(methodNode.desc)) {
                args.add(
                    LocalVariables.LocalVariable("arg${args.size}", argType.descriptor, null, null, null, args.size),
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
        matchType: Boolean = true,
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

    fun matchSourceLocals(
        sourceLocals: List<LocalVariables.SourceLocalVariable>
    ): Sequence<LocalVariables.SourceLocalVariable> {
        if (ordinal != null) {
            return sequenceOfNotNull(
                sourceLocals.asSequence().filter { it.type.isErasureEquivalentTo(type) }.drop(ordinal).firstOrNull()
            )
        }
        if (index != null) {
            return sequenceOfNotNull(sourceLocals.getOrNull(index))
        }
        if (names.isNotEmpty()) {
            return sourceLocals.asSequence().filter { it.mixinName in names }
        }

        // implicit mode
        return sequenceOfNotNull(
            sourceLocals.singleOrNull { it.type.isErasureEquivalentTo(type) }
        )
    }

    companion object {
        /**
         * Gets a [LocalInfo] from an annotation which declares the following attributes:
         * - `argsOnly` to only match the target method arguments
         * - `index` to match local variables by index
         * - `ordinal` to match local variables by type then index
         * - `name` to match local variables by name
         *
         * The `ModifyVariable` and `Local` annotations match this description.
         */
        fun fromAnnotation(localType: PsiType?, annotation: PsiAnnotation): LocalInfo {
            val argsOnly = annotation.findDeclaredAttributeValue("argsOnly")?.constantValue as? Boolean ?: false
            val index = (annotation.findDeclaredAttributeValue("index")?.constantValue as? Int)
                ?.takeIf { it != -1 }
            val ordinal = (annotation.findDeclaredAttributeValue("ordinal")?.constantValue as? Int)
                ?.takeIf { it != -1 }
            val names = annotation.findDeclaredAttributeValue("name")?.computeStringArray()?.toSet() ?: emptySet()
            return LocalInfo(localType, argsOnly, index, ordinal, names)
        }
    }
}
