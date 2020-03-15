/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.platform.mixin.config.MixinConfigFileType
import com.demonwav.mcdev.util.isPropertyKey
import com.demonwav.mcdev.util.isPropertyValue
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MixinConfigReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val pattern = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inFile(PlatformPatterns.psiFile().withFileType(StandardPatterns.`object`(MixinConfigFileType)))

        registrar.registerReferenceProvider(pattern.isPropertyKey(), ConfigProperty)
        registrar.registerReferenceProvider(pattern.isPropertyValue("package"), MixinPackage)
        registrar.registerReferenceProvider(pattern.isPropertyValue("plugin"), MixinPlugin)
        registrar.registerReferenceProvider(pattern.isPropertyValue("compatibilityLevel"), CompatibilityLevel)

        val mixinList = PlatformPatterns.psiElement(JsonArray::class.java)
        registrar.registerReferenceProvider(pattern.withParent(mixinList.isPropertyValue("mixins")), MixinClass)
        registrar.registerReferenceProvider(pattern.withParent(mixinList.isPropertyValue("server")), MixinClass)
        registrar.registerReferenceProvider(pattern.withParent(mixinList.isPropertyValue("client")), MixinClass)
    }
}
