/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.implements

import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.IMPLEMENTS
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class DuplicateInterfacePrefixInspection : MixinAnnotationAttributeInspection(IMPLEMENTS, null) {

    override fun getStaticDescription() = "Reports duplicate @Interface prefixes in an @Implements annotation."

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val interfaces = value.findAnnotations().ifEmpty { return }

        val prefixes = ArrayList<String>()
        for (iface in interfaces) {
            val prefixValue = iface.findDeclaredAttributeValue("prefix") ?: continue
            val prefix = prefixValue.constantStringValue ?: continue
            if (prefix in prefixes) {
                holder.registerProblem(prefixValue, "Duplicate prefix '$prefix'")
            } else {
                prefixes.add(prefix)
            }
        }
    }
}
