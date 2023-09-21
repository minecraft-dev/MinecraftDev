package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyReceiverHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedElementTypes = listOf(
        ElementType.METHOD_CALL, ElementType.FIELD_GET, ElementType.FIELD_SET
    )

    override fun extraTargetRestrictions(insn: AbstractInsnNode) = when (insn.opcode) {
        Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKEINTERFACE, Opcodes.GETFIELD, Opcodes.PUTFIELD -> true
        else -> false
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode
    ): Pair<ParameterGroup, PsiType>? {
        val params = getPsiParameters(insn, targetClass, annotation) ?: return null
        return ParameterGroup(params) to params[0].type
    }
}