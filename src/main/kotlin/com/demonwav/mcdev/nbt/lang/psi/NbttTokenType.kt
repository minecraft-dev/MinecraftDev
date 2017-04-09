/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi

import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class NbttTokenType(@NonNls debugName: String) : IElementType(debugName, NbttLanguage)

