/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.NbttLexer
import com.intellij.lexer.FlexAdapter

class NbttLexerAdapter : FlexAdapter(NbttLexer())
