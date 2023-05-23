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
