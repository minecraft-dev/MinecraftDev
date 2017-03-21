/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MavenLibraryPresentationProvider
import com.intellij.framework.library.LibraryVersionProperties

class BungeeCordPresentationProvider : MavenLibraryPresentationProvider(BUNGEECORD_LIBRARY_KIND, "net.md-5", "bungeecord-api") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.BUNGEECORD_ICON
}
