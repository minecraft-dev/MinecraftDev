/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.platform.LibraryModIdProvider
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiManager
import org.jetbrains.lang.manifest.psi.ManifestFile

class FabricLibraryModIdProvider : LibraryModIdProvider {
    override fun getModId(project: Project, library: Library): String? {
        val roots = library.getFiles(OrderRootType.CLASSES)

        // try to find mod id in fabric.mod.json first
        for (root in roots) {
            val fabricModJsonFile = root.findChild("fabric.mod.json") ?: continue
            val fabricModJson = PsiManager.getInstance(project).findFile(fabricModJsonFile) as? JsonFile ?: continue
            val rootObject = fabricModJson.topLevelValue as? JsonObject ?: continue
            val idProperty = rootObject.findProperty("id") ?: continue
            val id = idProperty.value as? JsonStringLiteral ?: continue
            return id.value
        }

        // check if we are in a split-source-set -client library, and then check for the mod id of the -common library
        val mavenCoords = JavaLibraryUtil.getMavenCoordinates(library) ?: return null
        if (!mavenCoords.artifactId.endsWith("-client")) {
            return null
        }
        val commonArtifactId =
            mavenCoords.artifactId.substring(0, mavenCoords.artifactId.length - "-client".length) + "-common"

        for (root in roots) {
            val manifestFile = root.findChild("META-INF")?.findChild("MANIFEST.MF") ?: continue
            val manifestPsi = PsiManager.getInstance(project).findFile(manifestFile) as? ManifestFile ?: continue
            val environmentNameHeader = manifestPsi.getHeader("Fabric-Loom-Split-Environment-Name") ?: continue
            val environmentName = environmentNameHeader.headerValue?.unwrappedText ?: continue
            if (environmentName != "client") {
                continue
            }

            var commonLibrary: Library? = null
            OrderEnumerator.orderEntries(project).forEachLibrary { lib ->
                val commonMavenCoords = JavaLibraryUtil.getMavenCoordinates(lib) ?: return@forEachLibrary true
                if (
                    commonMavenCoords.groupId != mavenCoords.groupId ||
                        commonMavenCoords.artifactId != commonArtifactId ||
                        commonMavenCoords.version != mavenCoords.version
                ) {
                    return@forEachLibrary true
                }
                commonLibrary = lib
                false
            }

            if (commonLibrary != null) {
                val commonModId = RecursionManager.doPreventingRecursion(commonLibrary!!, false) {
                    getModId(project, commonLibrary!!)
                }
                if (commonModId != null) {
                    return commonModId
                }
            }
        }

        return null
    }
}
