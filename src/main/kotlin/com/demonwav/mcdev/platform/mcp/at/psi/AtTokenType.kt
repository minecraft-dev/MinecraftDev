/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi

import com.demonwav.mcdev.platform.mcp.at.AtLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class AtTokenType(@NonNls debugName: String) : IElementType(debugName, AtLanguage)
