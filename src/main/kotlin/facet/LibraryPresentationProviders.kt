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

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.localFile
import com.demonwav.mcdev.util.manifest
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryKind
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION

abstract class ManifestLibraryPresentationProvider(
    kind: LibraryKind,
    private val title: String,
    private val startsWith: Boolean = false,
) :
    LibraryPresentationProvider<LibraryVersionProperties>(kind) {

    final override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue

            val title = manifest[IMPLEMENTATION_TITLE] ?: continue
            if (startsWith) {
                if (!title.startsWith(this.title)) {
                    continue
                }
            } else {
                if (title != this.title) {
                    continue
                }
            }

            val version = manifest[IMPLEMENTATION_VERSION] ?: continue
            return LibraryVersionProperties(version)
        }

        return null
    }
}

abstract class MavenLibraryPresentationProvider(
    kind: LibraryKind,
    private val groupId: String,
    private val artifactId: String,
    private val strict: Boolean = true,
) :
    LibraryPresentationProvider<LibraryVersionProperties>(kind) {

    private val propertiesPath = "META-INF/maven/$groupId/$artifactId/pom.properties"

    final override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = classesRoot.localFile
            val properties = JarUtil.loadProperties(file, propertiesPath) ?: continue

            if (strict && (properties["groupId"] != groupId || properties["artifactId"] != artifactId)) {
                continue
            }

            val version = properties["version"] as? String ?: continue
            return LibraryVersionProperties(version)
        }

        return null
    }
}
