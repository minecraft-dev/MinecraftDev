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

package com.demonwav.mcdev.toml.platform.forge.reference

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.toml.inDependenciesHeaderId
import com.demonwav.mcdev.toml.inModsTomlValueWithKey
import com.demonwav.mcdev.toml.stringValue
import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext
import kotlin.math.max
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlPsiFactory
import org.toml.lang.psi.TomlValue

class ModsTomlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(inDependenciesHeaderId(), ModsTomlDependencyIdReferenceProvider)
        registrar.registerReferenceProvider(inModsTomlValueWithKey("logoFile"), ModsTomlLogoFileReferenceProvider)
        registrar.registerReferenceProvider(inModsTomlValueWithKey("modId"), ModsTomlModIdReferenceProvider)
    }
}

object ModsTomlDependencyIdReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val keySegment = element as? TomlKeySegment ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(ModsTomlDependencyIdReference(keySegment))
    }
}

object ModsTomlLogoFileReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        val value = element as? TomlValue ?: return PsiReference.EMPTY_ARRAY
        val text = value.stringValue() ?: return PsiReference.EMPTY_ARRAY
        return LogoFileReferenceSet(value, text, 1, this).allReferences
    }
}

class LogoFileReferenceSet(element: PsiElement, text: String, offset: Int, provider: PsiReferenceProvider) :
    FileReferenceSet(text, element, offset, provider, true, true) {
    override fun computeDefaultContexts(): Collection<PsiFileSystemItem> {
        val rootManager = element.findModule()?.rootManager ?: return emptyList()

        val psiManager = element.manager
        return rootManager.getSourceRoots(JavaResourceRootType.RESOURCE)
            .mapNotNull { psiManager.findDirectory(it) }
    }

    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> =
        Condition { it.virtualFile.fileType.name == "Image" }
}

class ModsTomlDependencyIdReference(keySegment: TomlKeySegment) : PsiReferenceBase<TomlKeySegment>(keySegment) {
    override fun resolve(): PsiElement? {
        val referencedId = element.text
        return element.containingFile.childrenOfType<TomlArrayTable>()
            .filter { it.header.key?.segments?.firstOrNull()?.text == "mods" }.firstNotNullOfOrNull { table ->
                table.entries.find {
                    it.key.text == "modId" && it.value?.stringValue() == referencedId
                }?.value
            }
    }

    override fun getVariants(): Array<Any> =
        element.containingFile.childrenOfType<TomlArrayTable>()
            .filter { it.header.key?.segments?.firstOrNull()?.text == "mods" }
            .mapNotNull { table -> table.entries.find { it.key.text == "modId" }?.value?.stringValue() }
            .toTypedArray()

    override fun handleElementRename(newElementName: String): PsiElement =
        TomlPsiFactory(element.project).createKey(newElementName)

    override fun calculateDefaultRangeInElement(): TextRange =
        TextRange(0, element.textLength)
}

object ModsTomlModIdReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val value = element as? TomlValue ?: return PsiReference.EMPTY_ARRAY
        val stringValue = value.stringValue()
        if (stringValue != null && stringValue.startsWith("\${") && stringValue.endsWith("}")) {
            return PsiReference.EMPTY_ARRAY
        }

        return arrayOf(ModsTomlModIdReference(value))
    }
}

class ModsTomlModIdReference(element: TomlValue) :
    PsiReferenceBase<TomlValue>(element, TextRange(1, max(element.textLength - 1, 1))) {

    val modId: String? = element.stringValue()

    override fun resolve(): PsiElement? {
        if (modId.isNullOrBlank()) {
            return null
        }

        val scope = element.resolveScope
        if (modId == "minecraft") {
            return JavaPsiFacade.getInstance(element.project)
                .findClass("net.minecraftforge.fml.mclanguageprovider.MinecraftModContainer", scope)
        }

        val modAnnotation = JavaPsiFacade.getInstance(element.project).findClass(ForgeConstants.MOD_ANNOTATION, scope)
            ?: return null
        return AnnotatedElementsSearch.searchPsiClasses(modAnnotation, scope).mapFirstNotNull { modClass ->
            modClass.getAnnotation(ForgeConstants.MOD_ANNOTATION)
                ?.takeIf {
                    val id = it.findAttributeValue("value")?.constantStringValue
                    id == modId
                }
        }
    }

    override fun getVariants(): Array<Any> {
        val scope = element.resolveScope
        val modAnnotation = JavaPsiFacade.getInstance(element.project).findClass(ForgeConstants.MOD_ANNOTATION, scope)
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val modIds = mutableListOf(LookupElementBuilder.create("minecraft"))
        return AnnotatedElementsSearch.searchPsiClasses(modAnnotation, scope).mapNotNullTo(modIds) { modClass ->
            val modId = modClass.getAnnotation(ForgeConstants.MOD_ANNOTATION)
                ?.findAttributeValue("value")
                ?.constantStringValue
                ?: return@mapNotNullTo null

            JavaLookupElementBuilder.forClass(modClass, modId, true)
        }.toTypedArray()
    }
}
