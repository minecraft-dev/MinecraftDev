/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml.platform.forge.reference

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.toml.inDependenciesHeaderId
import com.demonwav.mcdev.toml.inModsTomlValueWithKey
import com.demonwav.mcdev.toml.stringValue
import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.mapFirstNotNull
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
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.ProcessingContext
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
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
            .filter { it.header.key?.segments?.firstOrNull()?.text == "mods" }
            .mapNotNull { table ->
                table.entries.find {
                    it.key.text == "modId" && it.value?.stringValue() == referencedId
                }?.value
            }
            .firstOrNull()
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
        return arrayOf(ModsTomlModIdReference(value))
    }
}

class ModsTomlModIdReference(element: TomlValue) :
    PsiReferenceBase<TomlValue>(element, TextRange(1, element.textLength - 1)) {

    val modId: String? = element.stringValue()

    override fun resolve(): PsiElement? {
        if (modId == null) {
            return null
        }
        val module = element.findModule() ?: return null
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        val modAnnotation = JavaPsiFacade.getInstance(element.project).findClass(ForgeConstants.MOD_ANNOTATION, scope)
            ?: return null
        val refScope = GlobalSearchScope.moduleScope(module)
        return ReferencesSearch.search(modAnnotation, refScope, true).mapFirstNotNull { ref ->
            ref.element.toUElement()?.getParentOfType<UAnnotation>()
                ?.takeIf {
                    it.qualifiedName == ForgeConstants.MOD_ANNOTATION &&
                        it.findAttributeValue("value")?.evaluateString() == modId
                }?.getContainingUClass()?.sourcePsi
        }
    }
}
