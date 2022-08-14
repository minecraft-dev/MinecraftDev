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

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.findSourceField
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.objectweb.asm.tree.ClassNode

interface MixinMemberAnnotationHandler : MixinAnnotationHandler {
    override fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): InsnResolutionInfo.Failure? {
        return if (resolveTarget(annotation, targetClass).isEmpty()) {
            InsnResolutionInfo.Failure()
        } else {
            null
        }
    }

    override fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement> {
        val targets = resolveTarget(annotation, targetClass)
        return targets.mapNotNull {
            when (it) {
                is FieldTargetMember ->
                    it.classAndField.field.findSourceField(
                        it.classAndField.clazz,
                        annotation.project,
                        annotation.resolveScope,
                        canDecompile = true
                    )
                is MethodTargetMember ->
                    it.classAndMethod.method.findSourceElement(
                        it.classAndMethod.clazz,
                        annotation.project,
                        annotation.resolveScope,
                        canDecompile = true
                    )
            }
        }
    }
}
