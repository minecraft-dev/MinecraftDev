/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.reference.target.FieldDefinitionReference
import com.demonwav.mcdev.platform.mixin.reference.target.MethodDefinitionReference
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
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
            InjectionPointReference.ELEMENT_PATTERN,
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

        // Definition references
        registrar.registerReferenceProvider(
            FieldDefinitionReference.ELEMENT_PATTERN,
            FieldDefinitionReference,
        )
        registrar.registerReferenceProvider(
            MethodDefinitionReference.ELEMENT_PATTERN,
            MethodDefinitionReference,
        )

        // Mixin targets references
        registrar.registerReferenceProvider(
            MixinTargetsReference.ELEMENT_PATTERN,
            MixinTargetsReference,
        )
    }
}
