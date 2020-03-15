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

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.framework.testParser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("NBTT Parsing Tests")
class NbttParsingTest : ProjectBuilderTest() {

    @Test
    @DisplayName("All Types Parsing Test")
    fun allTypesTest() = testParser("parser/fixtures/all_types.nbtt", ProjectBuilder::nbtt)
}
