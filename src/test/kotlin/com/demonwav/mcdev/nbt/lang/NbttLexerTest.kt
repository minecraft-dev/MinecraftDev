/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.framework.testLexer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NBTT Lexing Tests")
class NbttLexerTest {

    @Test
    @DisplayName("All Types Lexing Test")
    fun allTypesTest() = testLexer("lexer/fixtures/all_types.nbtt", NbttLexerAdapter())
}
