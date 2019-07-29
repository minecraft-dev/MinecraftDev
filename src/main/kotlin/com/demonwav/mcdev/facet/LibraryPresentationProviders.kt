/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
    private val startsWith: Boolean = false
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
    private val artifactId: String
) :
    LibraryPresentationProvider<LibraryVersionProperties>(kind) {

    private val propertiesPath = "META-INF/maven/$groupId/$artifactId/pom.properties"

    final override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = classesRoot.localFile
            val properties = JarUtil.loadProperties(file, propertiesPath) ?: continue

            if (properties["groupId"] != groupId || properties["artifactId"] != artifactId) {
                continue
            }

            val version = properties["version"] as? String ?: continue
            return LibraryVersionProperties(version)
        }

        return null
    }
}
