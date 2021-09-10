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

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class MethodOnlyInjectorHandler : InjectorAnnotationHandler() {
    override fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return insn is MethodInsnNode
    }
}
