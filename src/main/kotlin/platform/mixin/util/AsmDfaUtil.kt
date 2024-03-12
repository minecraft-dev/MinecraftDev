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

import com.demonwav.mcdev.util.internalName
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SimpleVerifier

object AsmDfaUtil {
    private val LOGGER = thisLogger()

    fun analyzeMethod(project: Project, classIn: ClassNode, methodIn: MethodNode): Array<Frame<BasicValue>?>? {
        return methodIn.cached(classIn, project) { clazz, method ->
            try {
                Analyzer(
                    PsiBytecodeInterpreter(
                        project,
                        Type.getObjectType(clazz.name),
                        clazz.superName?.let { Type.getObjectType(it) },
                        clazz.interfaces?.map { Type.getObjectType(it) } ?: emptyList(),
                        clazz.hasAccess(Opcodes.ACC_INTERFACE),
                    ),
                ).analyze(clazz.name, method)
            } catch (e: AnalyzerException) {
                val cause = e.cause
                if (cause is ProcessCanceledException) {
                    throw cause
                }
                LOGGER.warn("AsmDfaUtil.analyzeMethod failed", e)
                null
            }
        }
    }

    fun getLocalVariableType(
        project: Project,
        clazz: ClassNode,
        method: MethodNode,
        insn: AbstractInsnNode,
        slot: Int,
    ): Type? {
        val insns = method.instructions ?: return null
        val frames = analyzeMethod(project, clazz, method) ?: return null
        val frame = frames.getOrNull(insns.indexOf(insn)) ?: return null
        if (slot >= frame.locals) return null
        return frame.getLocal(slot)?.type
    }

    fun getLocalVariableTypes(
        project: Project,
        clazz: ClassNode,
        method: MethodNode,
        insn: AbstractInsnNode,
    ): Array<Type?>? {
        val insns = method.instructions ?: return null
        val frames = analyzeMethod(project, clazz, method) ?: return null
        val frame = frames.getOrNull(insns.indexOf(insn)) ?: return null
        return (0 until frame.locals).map { i -> frame.getLocal(i)?.type }.toTypedArray()
    }

    fun getStackType(
        project: Project,
        clazz: ClassNode,
        method: MethodNode,
        insn: AbstractInsnNode,
        depth: Int,
    ): Type? {
        val insns = method.instructions ?: return null
        val frames = analyzeMethod(project, clazz, method) ?: return null
        val frame = frames.getOrNull(insns.indexOf(insn)) ?: return null
        if (depth > frame.stackSize) return null
        return frame.getStack(frame.stackSize - 1 - depth)?.type
    }

    fun getStackTypes(
        project: Project,
        clazz: ClassNode,
        method: MethodNode,
        insn: AbstractInsnNode,
    ): Array<Type?>? {
        val insns = method.instructions ?: return null
        val frames = analyzeMethod(project, clazz, method) ?: return null
        val frame = frames.getOrNull(insns.indexOf(insn)) ?: return null
        return (0 until frame.stackSize).map { i -> frame.getStack(i)?.type }.toTypedArray()
    }

    private class PsiBytecodeInterpreter(
        private val project: Project,
        private val currentClass: Type,
        private val currentSuperClass: Type?,
        currentClassInterfaces: List<Type>,
        private val isInterface: Boolean,
    ) : SimpleVerifier(Opcodes.ASM7, currentClass, currentSuperClass, currentClassInterfaces, isInterface) {
        override fun getClass(type: Type?): Class<*> {
            // should never be called given we have overridden the other methods
            throw UnsupportedOperationException()
        }

        override fun isSubTypeOf(value: BasicValue, expected: BasicValue): Boolean {
            return isAssignableFrom(expected.type, value.type)
        }

        override fun isInterface(type: Type): Boolean {
            if (type == currentClass) {
                return isInterface
            }
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val psiType = type.toPsiType(elementFactory) as? PsiClassType ?: return false
            val clazz = psiType.resolve() ?: return false
            return clazz.isInterface
        }

        override fun getSuperClass(type: Type): Type? {
            if (type == currentClass) {
                return currentSuperClass
            }
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val psiType = type.toPsiType(elementFactory) as? PsiClassType ?: return null
            val clazz = psiType.resolve() ?: return null
            val superClass = clazz.superClass ?: return null
            val superClassName = superClass.internalName ?: return null
            return Type.getObjectType(superClassName)
        }

        override fun isAssignableFrom(type1: Type, type2: Type): Boolean {
            if (type1.descriptor == "Ljava/lang/Object;") {
                return true
            }
            if (type2.descriptor == "Lnull;") {
                return true
            }
            if (type1.sort == Type.ARRAY) {
                if (type2.sort != Type.ARRAY) {
                    return false
                }
                if (type1.dimensions != type2.dimensions) {
                    return false
                }
                return isAssignableFrom(type1.elementType, type2.elementType)
            }
            if (type1.sort == Type.OBJECT && type2.sort == Type.OBJECT) {
                val elementFactory = JavaPsiFacade.getElementFactory(project)
                val psiType1 = (type1.toPsiType(elementFactory) as? PsiClassType)?.resolve()
                    ?: return false
                val psiType2 = (type2.toPsiType(elementFactory) as? PsiClassType)?.resolve()
                    ?: return false
                return InheritanceUtil.isInheritorOrSelf(psiType2, psiType1, true)
            }
            return type2 == type1
        }
    }
}
