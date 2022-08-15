/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang.psi

import com.demonwav.mcdev.translations.lang.MCLangLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class LangElementType(@NonNls debugName: String) : IElementType(debugName, MCLangLanguage)
