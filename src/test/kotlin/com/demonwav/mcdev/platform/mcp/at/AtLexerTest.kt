/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.testLexer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer Lexing Tests")
class AtLexerTest {

    @Test
    @DisplayName("Spigot Mappings Lexing Test")
    fun spigotMappingsTest() = testLexer("lexer/fixtures/spigot_mappings_at.cfg", AtLexerAdapter())
}
