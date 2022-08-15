/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.adventure.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.adventure.AdventureConstants
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.SPECIFICATION_TITLE
import java.util.jar.Manifest

class AdventurePresentationProvider : LibraryPresentationProvider<DummyLibraryProperties>(ADVENTURE_LIBRARY_KIND) {
    override fun getIcon(properties: DummyLibraryProperties?) = PlatformAssets.ADVENTURE_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): DummyLibraryProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue

            if (findUsingManifest(manifest)) {
                return DummyLibraryProperties.INSTANCE
            }
        }

        return null
    }

    private fun findUsingManifest(manifest: Manifest): Boolean =
        manifest[SPECIFICATION_TITLE] == AdventureConstants.API_SPECIFICATION_TITLE ||
            manifest["Automatic-Module-Name"] == AdventureConstants.API_MODULE_ID
}
