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

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.util.isPropertyValue
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElement
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class FabricReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val stringInModJson = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inVirtualFile(PlatformPatterns.virtualFile().withName(FabricConstants.FABRIC_MOD_JSON))

        val entrypointsArray = PlatformPatterns.psiElement(JsonArray::class.java)
            .withSuperParent(2, PlatformPatterns.psiElement(JsonObject::class.java).isPropertyValue("entrypoints"))
        val entryPointSimplePattern = stringInModJson.withParent(entrypointsArray)
        val entryPointObjectPattern = stringInModJson.isPropertyValue("value")
            .withSuperParent(2, PlatformPatterns.psiElement(JsonObject::class.java).withParent(entrypointsArray))
        val entryPointPattern = StandardPatterns.or(entryPointSimplePattern, entryPointObjectPattern)
        registrar.registerReferenceProvider(entryPointPattern, EntryPointReference)

        val mixinConfigSimplePattern = stringInModJson.withParent(
            PlatformPatterns.psiElement(JsonArray::class.java).isPropertyValue("mixins"),
        )
        val mixinsConfigArray = PlatformPatterns.psiElement(JsonArray::class.java).isPropertyValue("mixins")
        val mixinConfigObjectPattern = stringInModJson.isPropertyValue("config")
            .withSuperParent(2, PlatformPatterns.psiElement(JsonElement::class.java).withParent(mixinsConfigArray))
        val mixinConfigPattern = StandardPatterns.or(mixinConfigSimplePattern, mixinConfigObjectPattern)
        registrar.registerReferenceProvider(
            mixinConfigPattern,
            ResourceFileReference(
                { MCDevBundle("reference.unresolved.mixin_config", it) },
                Regex("(.+)\\.mixins\\.json"),
            )
        )

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("accessWidener"),
            ResourceFileReference(
                { MCDevBundle("reference.unresolved.class_tweaker", it) },
                Regex("(.+)\\.(accesswidener|aw|classtweaker|ct)"),
            ),
        )

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("icon"),
            ResourceFileReference({ MCDevBundle("reference.unresolved.icon", it) }),
        )

        registrar.registerReferenceProvider(stringInModJson.isPropertyValue("license"), LicenseReference)
    }
}
