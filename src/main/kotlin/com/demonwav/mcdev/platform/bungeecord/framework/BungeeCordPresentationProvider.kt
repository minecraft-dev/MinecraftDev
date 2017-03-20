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
import com.intellij.openapi.roots.libraries.LibraryProperties

class BungeeCordPresentationProvider : MavenLibraryPresentationProvider(BUNGEECORD_LIBRARY_KIND, "net.md-5", "bungeecord-api") {
    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.BUNGEECORD_ICON
}
