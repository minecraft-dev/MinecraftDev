/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.facet.MinecraftFacetDetector
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mixin.config.MixinConfig
import com.demonwav.mcdev.platform.mixin.framework.MIXIN_LIBRARY_KIND
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.nullable
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import javax.swing.Icon

class MixinModule(facet: MinecraftFacet) : AbstractModule(facet) {
    val mixinVersion by nullable {
        var version = MinecraftFacetDetector.getLibraryVersions(facet.module)[MIXIN_LIBRARY_KIND]
            ?: return@nullable null
        // fabric mixin uses the format "0.10.4+mixin.0.8.4", return the original string otherwise.
        version = version.substringAfter("+mixin.")
        SemanticVersion.parse(version)
    }

    override val moduleType = MixinModuleType
    override val type = PlatformType.MIXIN
    override val icon: Icon? = null

    companion object {
        private val mixinFileType by lazy {
            FileTypeManager.getInstance().findFileTypeByName("Mixin Configuration") ?: FileTypes.UNKNOWN
        }

        fun getMixinConfigs(
            project: Project,
            scope: GlobalSearchScope
        ): Collection<MixinConfig> {
            return FileTypeIndex.getFiles(mixinFileType, scope)
                .mapNotNull {
                    (PsiManager.getInstance(project).findFile(it) as? JsonFile)?.topLevelValue as? JsonObject
                }
                .map { MixinConfig(project, it) }
        }

        fun getAllMixinClasses(
            project: Project,
            scope: GlobalSearchScope
        ): Collection<PsiClass> {
            return getMixinConfigs(project, scope).asSequence()
                .flatMap { (it.qualifiedMixins + it.qualifiedClient + it.qualifiedServer).asSequence() }
                .filterNotNull()
                .map { it.replace('$', '.') }
                .distinct()
                .flatMap { JavaPsiFacade.getInstance(project).findClasses(it, scope).asSequence() }
                .toList()
        }

        fun getBestWritableConfigForMixinClass(
            project: Project,
            scope: GlobalSearchScope,
            mixinClassName: String
        ): MixinConfig? {
            return getMixinConfigs(project, scope)
                .filter { it.isWritable && mixinClassName.startsWith("${it.pkg}.") }
                .maxByOrNull { it.pkg?.length ?: 0 }
        }
    }
}
