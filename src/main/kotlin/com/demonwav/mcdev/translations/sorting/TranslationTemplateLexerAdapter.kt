/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.sorting

import com.demonwav.mcdev.translations.lang.gen.TranslationTemplateLexer
import com.intellij.lexer.FlexAdapter

class TranslationTemplateLexerAdapter : FlexAdapter(TranslationTemplateLexer())
