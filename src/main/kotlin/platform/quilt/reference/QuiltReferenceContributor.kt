/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt.reference

import com.demonwav.mcdev.platform.fabric.reference.EntryPointReference
import com.demonwav.mcdev.platform.fabric.reference.LicenseReference
import com.demonwav.mcdev.platform.fabric.reference.ResourceFileReference
import com.demonwav.mcdev.platform.quilt.util.QuiltConstants
import com.demonwav.mcdev.util.isPropertyValue
import com.intellij.json.psi.*
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.completion.or


class QuiltReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val stringInModJson = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inVirtualFile(PlatformPatterns.virtualFile().withName(QuiltConstants.QUILT_MOD_JSON))

        val entryPointPattern = stringInModJson.withParent(
            PlatformPatterns.psiElement(JsonArray::class.java)
                .withSuperParent(
                    2,
                    PlatformPatterns.psiElement(JsonObject::class.java).isPropertyValue("entrypoints"),
                ),
        ) or stringInModJson.withSuperParent(2, PlatformPatterns.psiElement(JsonObject::class.java).isPropertyValue("entrypoints"))

        registrar.registerReferenceProvider(entryPointPattern, EntryPointReference)

        val mixinConfigPattern = stringInModJson.withParent(
            PlatformPatterns.psiElement(JsonArray::class.java).isPropertyValue("mixin"),
        ) or stringInModJson.with(
            object : PatternCondition<JsonElement>("isPropertyValue") {
                override fun accepts(t: JsonElement, context: ProcessingContext?): Boolean {
                    val parent = t.parent as? JsonProperty ?: return false
                    return parent.value == t && parent.name == "mixin"
                }
            }
        )

        registrar.registerReferenceProvider(mixinConfigPattern, ResourceFileReference("mixin config '%s'"))

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("access_widener"),
            ResourceFileReference("access widener '%s'"),
        )

        registrar.registerReferenceProvider(
            stringInModJson.isPropertyValue("icon"),
            ResourceFileReference("icon '%s'"),
        )

        registrar.registerReferenceProvider(stringInModJson.isPropertyValue("license"), LicenseReference)
    }
}
