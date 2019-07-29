/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.i18n.lang.gen.I18nLexer
import com.intellij.lexer.FlexAdapter

class I18nLexerAdapter : FlexAdapter(I18nLexer())
