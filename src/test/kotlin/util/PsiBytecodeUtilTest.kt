/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.framework.EdtInterceptor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("bytecode-utils Extension Property Tests")
class PsiBytecodeUtilTest : OuterClassTest() {

    @Test
    @DisplayName("internalName Of Outer Class Test")
    fun outerInternalNameTest() = Assertions.assertEquals("com/example/test/OuterClass", outerClass.internalName)

    @Test
    @DisplayName("internalName Of Outer Anonymous Class Test")
    fun outerAnonymousInternalNameTest() =
        Assertions.assertEquals("com/example/test/OuterClass$1", outerAnonymousClass.internalName)

    @Test
    @DisplayName("internalName Of Inner Class Test")
    fun innerInternalNameTest() =
        Assertions.assertEquals("com/example/test/OuterClass\$InnerClass", innerClass.internalName)

    @Test
    @DisplayName("internalName Of Inner Anonymous Class Test")
    fun innerAnonymousInternalNameTest() =
        Assertions.assertEquals("com/example/test/OuterClass\$InnerClass$1", innerAnonymousClass.internalName)

    @Test
    @DisplayName("internalName Of Inner Anonymous Inner Class Test")
    fun innerAnonymousInnerInternalNameTest() =
        Assertions.assertEquals(
            "com/example/test/OuterClass\$InnerClass$1\$AnonymousInnerClass",
            innerAnonymousInnerClass.internalName,
        )
}
