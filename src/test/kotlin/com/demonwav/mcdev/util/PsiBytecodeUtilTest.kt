/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
            innerAnonymousInnerClass.internalName
        )
}
