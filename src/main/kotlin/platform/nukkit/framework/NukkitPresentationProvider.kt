/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.nukkit.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MavenLibraryPresentationProvider
import com.intellij.framework.library.LibraryVersionProperties

class NukkitPresentationProvider : MavenLibraryPresentationProvider(NUKKIT_LIBRARY_KIND, "cn.nukkit", "nukkit", true) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.NUKKIT_ICON
}
