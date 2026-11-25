/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.framework.testParser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Parsing Tests")
class UnpickParsingTest : ProjectBuilderTest() {

    @Test
    @DisplayName("Spec Example Parsing Test")
    fun specTest() = testParser("parser/fixtures/spec.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Expression Parsing Test")
    fun expressionTest() = testParser("parser/fixtures/expression.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Group Definition Parsing Test")
    fun groupDefinitionTest() = testParser("parser/fixtures/group_definition.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Target Method Parsing Test")
    fun targetMethodTest() = testParser("parser/fixtures/target_method.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Minimal Spaces Parsing Test")
    fun minimalSpacesTest() = testParser("parser/fixtures/minimal_spaces.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Doc Comments Parsing Test")
    fun docsTest() = testParser("parser/fixtures/docs.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Error Recovery Parsing Test")
    fun recoveryTest() = testParser("parser/fixtures/recovery.unpick", ProjectBuilder::unpick)

    @Test
    @DisplayName("Unsupported Version Parsing Test")
    fun unsupportedTest() = testParser("parser/fixtures/unsupported.unpick", ProjectBuilder::unpick)
}
