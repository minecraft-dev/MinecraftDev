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

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode

class RedirectInjectorHandler : InjectorAnnotationHandler() {
    override fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return when (insn) {
            is FieldInsnNode -> true
            is MethodInsnNode -> {
                insn.name != "<init>"
            }
            is InsnNode -> {
                insn.opcode == Opcodes.ARRAYLENGTH ||
                    insn.opcode in Opcodes.IALOAD..Opcodes.SALOAD ||
                    insn.opcode in Opcodes.IASTORE..Opcodes.SASTORE
            }
            is TypeInsnNode -> {
                insn.opcode == Opcodes.NEW || insn.opcode == Opcodes.INSTANCEOF
            }
            else -> false
        }
    }
}
