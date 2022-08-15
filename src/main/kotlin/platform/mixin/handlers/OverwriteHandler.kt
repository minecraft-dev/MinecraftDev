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

import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.util.memberReference
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.tree.ClassNode

class OverwriteHandler : MixinMemberAnnotationHandler {
    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        val method = annotation.parentOfType<PsiMethod>() ?: return emptyList()
        val targetMethod = targetClass.findMethod(method.memberReference) ?: return emptyList()
        return listOf(MethodTargetMember(targetClass, targetMethod))
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        val method = annotation.parentOfType<PsiMethod>() ?: return null
        return "Unresolved method ${method.name} in target class"
    }

    override val isEntryPoint = true

    companion object {
        fun getInstance(): OverwriteHandler? {
            return MixinAnnotationHandler.forMixinAnnotation(OVERWRITE) as? OverwriteHandler
        }
    }
}
