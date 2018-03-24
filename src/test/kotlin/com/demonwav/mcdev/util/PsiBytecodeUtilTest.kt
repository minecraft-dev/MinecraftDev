/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

class PsiBytecodeUtilTest : OuterClassTest() {

    fun `test outer internal name`() = assertEquals("com/example/test/OuterClass", outerClass.internalName)
    fun `test outer anonymous internal name`() = assertEquals("com/example/test/OuterClass$1", outerAnonymousClass.internalName)
    fun `test inner internal name`() = assertEquals("com/example/test/OuterClass\$InnerClass", innerClass.internalName)
    fun `test inner anonymous internal name`() =
            assertEquals("com/example/test/OuterClass\$InnerClass$1", innerAnonymousClass.internalName)
    fun `test inner anonymous inner internal name`() =
            assertEquals("com/example/test/OuterClass\$InnerClass$1\$AnonymousInnerClass", innerAnonymousInnerClass.internalName)
}
