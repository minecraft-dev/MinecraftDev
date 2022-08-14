/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
                    PlatformPatterns.psiElement(JsonObject::class.java).isPropertyValue("entrypoints")
                )
        )
        registrar.registerReferenceProvider(entryPointPattern, EntryPointReference)

        val mixinConfigPattern = stringInModJson.withParent(
            PlatformPatterns.psiElement(JsonArray::class.java).isPropertyValue("mixins")
        )
        registrar.registerReferenceProvider(mixinConfigPattern, ResourceFileReference("mixin config '%s'"))

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("accessWidener"),
            ResourceFileReference("access widener '%s'")
        )

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("icon"),
            ResourceFileReference("icon '%s'")
        )

        registrar.registerReferenceProvider(stringInModJson.isPropertyValue("license"), LicenseReference)
    }
}
