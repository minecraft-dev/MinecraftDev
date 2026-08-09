package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.patterns.PsiJavaElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiLiteral

object MixinReferences {
    val MIXIN_TARGETS: PsiJavaElementPattern.Capture<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(MIXIN, "targets")
}
