/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression.gui

import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.shortDescString
import com.demonwav.mcdev.platform.mixin.util.shortName
import com.demonwav.mcdev.platform.mixin.util.textify
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationInfo
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.StringConcatInfo
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

fun FlowValue.shortString(project: Project, clazz: ClassNode, method: MethodNode): String {
    if (isComplex) {
        return "Complex Flow (type ${type.shortName})"
    }
    ExpressionASMUtils.getConstant(insn)?.let { return constantString(it) }
    ExpressionASMUtils.getCastType(insn)?.let { return castString(it) }
    newArrayString(this)?.let { return it }
    getDecoration<MethodCallType>(FlowDecorations.METHOD_CALL_TYPE)?.let { methodCallType ->
        return methodString(insn as MethodInsnNode, methodCallType)
    }
    getDecoration<InstantiationInfo>(FlowDecorations.INSTANTIATION_INFO)?.let { info ->
        return instantiationString(info)
    }
    getDecoration<StringConcatInfo>(FlowDecorations.STRING_CONCAT_INFO)?.let { _ ->
        return "+"
    }
    getDecoration<LMFInfo>(FlowDecorations.LMF_INFO)?.let { info ->
        return lmfString(info)
    }
    getDecoration<FlowValue>(FlowDecorations.COMPLEX_COMPARISON_JUMP)?.let { jump ->
        return complexCmpString(insn.opcode, jump.insn.opcode)
    }
    if (insn.opcode == Opcodes.INSTANCEOF) {
        return instanceofString(insn as TypeInsnNode)
    }
    return when (val insn = insn) {
        is FieldInsnNode -> fieldString(insn)
        is VarInsnNode -> varString(this, insn.`var`, project, clazz, method)
        else -> opcodeString(insn.opcode) ?: longString()
    }
}

fun FlowValue.longString(): String =
    if (isComplex) {
        "Complex Flow (type ${type.className})"
    } else {
        insn.textify()
    }

private fun shortOwner(owner: String) = Type.getObjectType(owner).shortName

private fun constantString(cst: Any): String {
    if (cst is Int) {
        return when (cst) {
            0 -> "0 or false"
            1 -> "1 or true"
            in Char.MIN_VALUE.code..Char.MAX_VALUE.code -> "$cst or '${cst.toChar().toString().escape()}'"
            else -> cst.toString()
        }
    }
    return when (cst) {
        Type.VOID_TYPE -> "null"
        is Type -> "${cst.shortName}.class"
        is String -> "'${cst.escape()}'"
        is Float -> "${cst}F"
        is Long -> "${cst}L"
        else -> cst.toString()
    }
}

private fun instantiationString(info: InstantiationInfo): String {
    val start = "new ${info.type.shortName}"
    val insn = info.initCall
    if (insn.isComplex) {
        return start
    }
    val call = insn.insn as? MethodInsnNode ?: return start
    return start + shortDescString(call.desc)
}

private fun lmfString(info: LMFInfo): String {
    val handle = info.impl
    return when (info.type!!) {
        LMFInfo.Type.FREE_METHOD -> "${shortOwner(handle.owner)}::${handle.name}"
        LMFInfo.Type.BOUND_METHOD -> "::${handle.name}"
        LMFInfo.Type.INSTANTIATION -> "${shortOwner(handle.owner)}::new"
    }
}

private fun varString(flow: FlowValue, index: Int, project: Project, clazz: ClassNode, method: MethodNode): String {
    var location = InsnExpander.getRepresentative(flow)
    if (location.opcode in Opcodes.ISTORE..Opcodes.ASTORE) {
        location = location.next
    }
    val localName = ReadAction.computeBlocking<_, Nothing> {
        runCatching {
            LocalVariables.getLocalVariableAt(project, clazz, method, location, index)
        }.getOrNull()
    }?.name ?: "<local $index>"
    val isStore = flow.insn.opcode in Opcodes.ISTORE..Opcodes.ASTORE
    return localName + if (isStore) " =" else ""
}

private fun fieldString(insn: FieldInsnNode): String {
    val isStatic = insn.opcode in Opcodes.GETSTATIC..Opcodes.PUTSTATIC
    val isWrite = insn.opcode == Opcodes.PUTFIELD || insn.opcode == Opcodes.PUTSTATIC
    val owner = if (isStatic) shortOwner(insn.owner) else ""
    return "$owner.${insn.name}" + if (isWrite) " =" else ""
}

private fun methodString(insn: MethodInsnNode, methodCallType: MethodCallType): String {
    val owner = when (methodCallType) {
        MethodCallType.NORMAL -> ""
        MethodCallType.SUPER -> "super"
        MethodCallType.STATIC -> shortOwner(insn.owner)
    }
    return "$owner.${insn.name}${shortDescString(insn.desc)}"
}

private fun castString(type: Type): String = "(${type.shortName})"

private fun complexCmpString(opcode: Int, jumpOpcode: Int): String {
    val isG = opcode == Opcodes.FCMPG || opcode == Opcodes.DCMPG
    val isLong = opcode == Opcodes.LCMP
    return when (jumpOpcode) {
        Opcodes.IFEQ, Opcodes.IFNE -> "== or !="
        Opcodes.IFLT, Opcodes.IFGE -> when {
            isLong -> "< or >="
            isG -> "<"
            else -> ">="
        }
        Opcodes.IFGT, Opcodes.IFLE -> when {
            isLong -> "<= or >"
            isG -> "<="
            else -> ">"
        }
        else -> "Unknown jump"
    }
}

private fun newArrayString(flow: FlowValue): String? {
    val insn = flow.insn
    val type = when (insn.opcode) {
        Opcodes.NEWARRAY, Opcodes.ANEWARRAY -> ExpressionASMUtils.getUnaryType(insn)
        Opcodes.MULTIANEWARRAY -> ExpressionASMUtils.getNaryType(insn)
        else -> return null
    }
    val prefix = "new ${type.shortName}"
    if (!flow.hasDecoration(FlowDecorations.ARRAY_CREATION_INFO)) {
        return prefix
    }
    return "$prefix{ }"
}

private fun instanceofString(insn: TypeInsnNode): String {
    val type = Type.getObjectType(insn.desc)
    return "instanceof ${type.shortName}"
}

private fun opcodeString(opcode: Int) = when (opcode) {
    Opcodes.ATHROW -> "throw"
    in Opcodes.IRETURN..Opcodes.RETURN -> "return"
    in Opcodes.IADD..Opcodes.DADD -> "+"
    in Opcodes.ISUB..Opcodes.DSUB -> "-"
    in Opcodes.IMUL..Opcodes.DMUL -> "*"
    in Opcodes.IDIV..Opcodes.DDIV -> "/"
    in Opcodes.IREM..Opcodes.DREM -> "%"
    in Opcodes.INEG..Opcodes.DNEG -> "-"
    Opcodes.ISHL, Opcodes.LSHL -> "<<"
    Opcodes.ISHR, Opcodes.LSHR -> ">>"
    Opcodes.IUSHR, Opcodes.LUSHR -> ">>>"
    Opcodes.IAND, Opcodes.LAND -> "&"
    Opcodes.IOR, Opcodes.LOR -> "|"
    Opcodes.IXOR, Opcodes.LXOR -> "^"
    Opcodes.IF_ACMPEQ, Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPNE, Opcodes.IF_ICMPNE -> "== or !="
    Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE -> "< or >="
    Opcodes.IF_ICMPLE, Opcodes.IF_ICMPGT -> "<= or >"
    Opcodes.ARRAYLENGTH -> ".length"
    in Opcodes.IALOAD..Opcodes.SALOAD -> "[]"
    in Opcodes.IASTORE..Opcodes.SASTORE -> "[] ="
    else -> null
}

private fun String.escape() =
    StringUtil.escapeStringCharacters(this)
        .replace("'", "\\\\'")
