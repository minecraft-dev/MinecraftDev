/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.psi

import com.demonwav.mcdev.platform.mcp.aw.AwLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class AwTokenType(@NonNls debugName: String) : IElementType(debugName, AwLanguage)
