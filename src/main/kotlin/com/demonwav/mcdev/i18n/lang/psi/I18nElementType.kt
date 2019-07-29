/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.psi

import com.demonwav.mcdev.i18n.lang.I18nLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class I18nElementType(@NonNls debugName: String) : IElementType(debugName, I18nLanguage)
