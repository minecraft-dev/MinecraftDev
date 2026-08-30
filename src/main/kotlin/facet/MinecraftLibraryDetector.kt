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

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.Properties
import java.util.jar.Manifest

interface MinecraftLibraryDetector {
    val platformType: PlatformType

    fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean

    companion object {
        @JvmStatic
        val EP_NAME = ExtensionPointName.create<MinecraftLibraryDetector>(
            "com.demonwav.minecraft-dev.minecraftLibraryDetector",
        )

        fun isLibraryPresent(
            platformType: PlatformType,
            project: Project,
            scope: GlobalSearchScope,
        ): Boolean = EP_NAME.extensionList.any {
            it.platformType == platformType && it.isLibraryPresent(project, scope)
        }
    }
}

abstract class ClassMinecraftLibraryDetector(
    final override val platformType: PlatformType,
    private val qualifiedClassName: String,
) : MinecraftLibraryDetector {
    override fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean =
        JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, scope) != null
}

abstract class MavenMinecraftLibraryDetector(
    final override val platformType: PlatformType,
    private val groupId: String,
    private val artifactId: String,
    private val strict: Boolean = true,
) : MinecraftLibraryDetector {
    private val propertiesPath = "META-INF/maven/$groupId/$artifactId/pom.properties"

    final override fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean =
        filesByName("pom.properties", scope).any { file ->
            if (!file.hasRelativePath(propertiesPath)) {
                return@any false
            }

            runCatching {
                val properties = Properties()
                file.inputStream.use(properties::load)

                (!strict ||
                    properties.getProperty("groupId") == groupId &&
                    properties.getProperty("artifactId") == artifactId) &&
                    properties.getProperty("version") != null
            }.getOrDefault(false)
        }
}

fun hasLibraryFile(
    name: String,
    relativePath: String,
    scope: GlobalSearchScope,
    predicate: (VirtualFile) -> Boolean = { true },
): Boolean = filesByName(name, scope).any { file ->
    file.hasRelativePath(relativePath) && predicate(file)
}

fun hasLibraryManifest(scope: GlobalSearchScope, predicate: (Manifest) -> Boolean): Boolean =
    hasLibraryFile("MANIFEST.MF", "META-INF/MANIFEST.MF", scope) { file ->
        runCatching {
            file.inputStream.use { input -> predicate(Manifest(input)) }
        }.getOrDefault(false)
    }

private fun filesByName(name: String, scope: GlobalSearchScope): Collection<VirtualFile> =
    FilenameIndex.getVirtualFilesByName(name, scope)

private fun VirtualFile.hasRelativePath(relativePath: String): Boolean {
    val normalizedPath = path.replace('\\', '/')
    val normalizedRelativePath = relativePath.replace('\\', '/')
    return normalizedPath.endsWith("!/$normalizedRelativePath") ||
        normalizedPath.endsWith("/$normalizedRelativePath")
}
