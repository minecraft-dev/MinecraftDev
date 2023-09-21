package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class WrapWithConditionHandler : MixinExtrasInjectorAnnotationHandler() {
    override val oldSuperBehaviour = true

    override val supportedElementTypes = listOf(
        ElementType.METHOD_CALL, ElementType.FIELD_SET
    )

    override fun extraTargetRestrictions(insn: AbstractInsnNode) = getInsnReturnType(insn) == Type.VOID_TYPE

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode
    ): Pair<ParameterGroup, PsiType>? {
        val params = getPsiParameters(insn, targetClass, annotation) ?: return null
        return ParameterGroup(params) to PsiType.BOOLEAN
    }
}