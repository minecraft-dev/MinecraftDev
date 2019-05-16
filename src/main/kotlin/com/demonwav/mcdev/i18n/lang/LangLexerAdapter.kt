/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.i18n.lang.gen.LangLexer
import com.intellij.lexer.FlexAdapter

class LangLexerAdapter : FlexAdapter(LangLexer())
