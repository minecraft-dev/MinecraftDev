/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

class PsiBytecodeUtilTest : OuterClassTest() {

    fun testOuterInternalName() = assertEquals("com/example/test/OuterClass", outerClass.internalName)
    fun testOuterAnonymousInternalName() = assertEquals("com/example/test/OuterClass$1", outerAnonymousClass.internalName)
    fun testInnerInternalName() = assertEquals("com/example/test/OuterClass\$InnerClass", innerClass.internalName)
    fun testInnerAnonymousInternalName() =
            assertEquals("com/example/test/OuterClass\$InnerClass$1", innerAnonymousClass.internalName)
    fun testInnerAnonymousInnerInternalName() =
            assertEquals("com/example/test/OuterClass\$InnerClass$1\$AnonymousInnerClass", innerAnonymousInnerClass.internalName)
}
