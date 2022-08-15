/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameValuePair

abstract class MixinAnnotationAttributeInspection(
    private val annotation: String?,
    private val attribute: String?
) : MixinInspection() {

    constructor(attribute: String?) : this(null, attribute)

    protected abstract fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    )

    final override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private inner class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            if (
                pair.name != attribute &&
                (attribute != null || pair.name != PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME)
            ) {
                return
            }

            val psiAnnotation = pair.annotationFromNameValuePair ?: return

            if (annotation != null && !psiAnnotation.hasQualifiedName(annotation)) {
                return
            }

            val value = pair.value ?: return
            visitAnnotationAttribute(psiAnnotation, value, this.holder)
        }
    }
}
