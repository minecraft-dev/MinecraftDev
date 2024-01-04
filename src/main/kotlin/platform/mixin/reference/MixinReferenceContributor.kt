/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MixinReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Method references
        registrar.registerReferenceProvider(
            MethodReference.ELEMENT_PATTERN,
            MethodReference,
        )

        // Desc references
        registrar.registerReferenceProvider(
            DescReference.ELEMENT_PATTERN,
            DescReference,
        )

        // Injection point types
        registrar.registerReferenceProvider(
            PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                .insideAnnotationAttribute(AT),
            InjectionPointReference,
        )

        // Target references
        registrar.registerReferenceProvider(
            TargetReference.ELEMENT_PATTERN,
            TargetReference,
        )

        // Accessor references
        registrar.registerReferenceProvider(
            AccessorReference.ELEMENT_PATTERN,
            AccessorReference,
        )
        registrar.registerReferenceProvider(
            InvokerReference.ELEMENT_PATTERN,
            InvokerReference,
        )
    }
}
