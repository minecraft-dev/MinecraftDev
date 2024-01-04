/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.isPropertyValue
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class FabricReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val stringInModJson = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inVirtualFile(PlatformPatterns.virtualFile().withName(FabricConstants.FABRIC_MOD_JSON))

        val entryPointPattern = stringInModJson.withParent(
            PlatformPatterns.psiElement(JsonArray::class.java)
                .withSuperParent(
                    2,
                    PlatformPatterns.psiElement(JsonObject::class.java).isPropertyValue("entrypoints"),
                ),
        )
        registrar.registerReferenceProvider(entryPointPattern, EntryPointReference)

        val mixinConfigPattern = stringInModJson.withParent(
            PlatformPatterns.psiElement(JsonArray::class.java).isPropertyValue("mixins"),
        )
        registrar.registerReferenceProvider(mixinConfigPattern, ResourceFileReference("mixin config '%s'"))

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("accessWidener"),
            ResourceFileReference("access widener '%s'"),
        )

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("icon"),
            ResourceFileReference("icon '%s'"),
        )

        registrar.registerReferenceProvider(stringInModJson.isPropertyValue("license"), LicenseReference)
    }
}
