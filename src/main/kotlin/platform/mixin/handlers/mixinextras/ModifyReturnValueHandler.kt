package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiType
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ModifyReturnValueHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedElementTypes = listOf(ElementType.RETURN)

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode
    ): Pair<ParameterGroup, PsiType>? {
        val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)
        return ParameterGroup(listOf(Parameter("original", returnType))) to returnType
    }
}