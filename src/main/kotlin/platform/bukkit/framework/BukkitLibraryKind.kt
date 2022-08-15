/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.framework

import com.demonwav.mcdev.util.libraryKind
import com.intellij.openapi.roots.libraries.LibraryKind

val BUKKIT_LIBRARY_KIND: LibraryKind = libraryKind("bukkit-api")
val SPIGOT_LIBRARY_KIND: LibraryKind = libraryKind("spigot-api")
val PAPER_LIBRARY_KIND: LibraryKind = libraryKind("paper-api")
