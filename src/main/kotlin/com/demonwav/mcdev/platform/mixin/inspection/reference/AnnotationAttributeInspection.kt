/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameValuePair

abstract class AnnotationAttributeInspection(private val annotation: List<String>, private val attribute: String) : MixinInspection() {

    constructor(annotation: String, attribute: String) : this(listOf(annotation), attribute)

    protected abstract fun visitAnnotationAttribute(annotation: PsiAnnotation, value: PsiAnnotationMemberValue, holder: ProblemsHolder)

    override final fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private inner class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            if (pair.name != attribute) {
                return
            }

            val psiAnnotation = pair.annotationFromNameValuePair ?: return
            if (psiAnnotation.qualifiedName !in annotation) {
                return
            }

            val value = pair.value ?: return
            visitAnnotationAttribute(psiAnnotation, value, this.holder)
        }

    }

}
