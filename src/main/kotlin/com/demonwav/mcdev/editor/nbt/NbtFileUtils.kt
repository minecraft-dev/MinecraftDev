/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.editor.nbt

import com.intellij.openapi.vfs.VirtualFile

val VirtualFile.isNbtFile
    get() = this.name.endsWith(".nbt") // TODO do more complex checks
