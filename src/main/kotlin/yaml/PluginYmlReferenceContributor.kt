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

package com.demonwav.mcdev.yaml

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.bukkit.PaperModuleType
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ObjectPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.ArrayUtilRt
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

private fun <P : ObjectPattern<out PsiElement, P>> P.inSpigotOrPaperPluginYml(): P = with(
    object : PatternCondition<PsiElement>("") {
        override fun accepts(t: PsiElement, context: ProcessingContext): Boolean {
            val module = t.findModule() ?: return false
            val instance = MinecraftFacet.getInstance(module, SpigotModuleType, PaperModuleType) ?: return false
            return instance.pluginYml == t.containingFile.originalFile.virtualFile
        }
    }
)

private fun <P : ObjectPattern<out PsiElement, P>> P.inBungeePluginYml(): P = with(
    object : PatternCondition<PsiElement>("") {
        override fun accepts(t: PsiElement, context: ProcessingContext): Boolean {
            val module = t.findModule() ?: return false
            val instance = MinecraftFacet.getInstance(module, BungeeCordModuleType) ?: return false
            return instance.pluginYml == t.containingFile.originalFile.virtualFile
        }
    }
)

class PluginYmlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java)
                .withParent(PlatformPatterns.psiElement(YAMLKeyValue::class.java).withName("main"))
                .inSpigotOrPaperPluginYml(),
            PluginYmlClassReferenceProvider(BukkitConstants.PLUGIN)
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java)
                .withParent(PlatformPatterns.psiElement(YAMLKeyValue::class.java).withName("main"))
                .inBungeePluginYml(),
            PluginYmlClassReferenceProvider(BungeeCordConstants.PLUGIN)
        )
    }
}

class PluginYmlClassReferenceProvider(val superClass: String) : JavaClassReferenceProvider() {

    init {
        setOption(ALLOW_DOLLAR_NAMES, true)
        setOption(JVM_FORMAT, true)
        setOption(CONCRETE, true)
        setOption(INSTANTIATABLE, true)
        setOption(SUPER_CLASSES, listOf(superClass))
        setOption(ADVANCED_RESOLVE, true)
    }

    override fun getReferencesByString(
        str: String,
        position: PsiElement,
        offsetInPosition: Int
    ): Array<out PsiReference?> {
        return object : JavaClassReferenceSet(str, position, offsetInPosition, true, this) {
            override fun isAllowDollarInNames(): Boolean = true

            override fun isAllowSpaces(): Boolean = false

            override fun createReference(
                referenceIndex: Int,
                referenceText: String,
                textRange: TextRange,
                staticImport: Boolean
            ): JavaClassReference {
                return PluginYmlClassReference(this, referenceIndex, referenceText, textRange, staticImport, superClass)
            }
        }.allReferences
    }
}

class PluginYmlClassReference(
    referenceSet: JavaClassReferenceSet,
    referenceIndex: Int,
    referenceText: String,
    textRange: TextRange,
    staticImport: Boolean,
    val superClass: String
) : JavaClassReference(referenceSet, textRange, referenceIndex, referenceText, staticImport) {

    override fun getVariants(): Array<out Any?> {
        val pluginClass =
            JavaPsiFacade.getInstance(element.project).findClass(superClass, element.resolveScope)
                ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        val candidates =
            ClassInheritorsSearch.search(pluginClass, element.resolveScope, true)
                .filter { !it.hasModifier(JvmModifier.ABSTRACT) }
                .map { JavaLookupElementBuilder.forClass(it, it.qualifiedName) }
                .toTypedArray()
        return candidates
    }
}
