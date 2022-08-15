/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang

import com.demonwav.mcdev.translations.lang.gen.LangLexer
import com.intellij.lexer.FlexAdapter

class LangLexerAdapter : FlexAdapter(LangLexer())
