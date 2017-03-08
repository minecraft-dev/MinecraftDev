/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.framework

import com.intellij.openapi.roots.libraries.LibraryKind

@JvmField
val CANARY_LIBRARY_KIND: LibraryKind = LibraryKind.create("canary-api")

@JvmField
val NEPTUNE_LIBRARY_KIND: LibraryKind = LibraryKind.create("neptune-api")
