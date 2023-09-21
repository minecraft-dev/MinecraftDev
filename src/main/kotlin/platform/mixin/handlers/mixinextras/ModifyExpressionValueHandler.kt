package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyExpressionValueHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedElementTypes = listOf(
        ElementType.METHOD_CALL, ElementType.FIELD_GET, ElementType.INSTANTIATION, ElementType.CONSTANT
    )

    override fun extraTargetRestrictions(insn: AbstractInsnNode) = getInsnReturnType(insn)?.equals(Type.VOID_TYPE) == false

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode
    ): Pair<ParameterGroup, PsiType>? {
        val psiType = getPsiReturnType(insn, annotation) ?: return null
        return ParameterGroup(listOf(Parameter("original", psiType))) to psiType
    }
}