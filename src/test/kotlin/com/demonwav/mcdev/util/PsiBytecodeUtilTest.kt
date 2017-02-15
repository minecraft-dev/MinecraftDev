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

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

class PsiBytecodeUtilTest : LightCodeInsightFixtureTestCase() {

    private lateinit var outerClass: PsiClass
    private lateinit var outerAnonymousClass: PsiAnonymousClass
    private lateinit var innerClass: PsiClass
    private lateinit var innerAnonymousClass: PsiAnonymousClass
    private lateinit var innerAnonymousInnerClass: PsiClass

    override fun setUp() {
        super.setUp()
        this.outerClass = (myFixture.configureByFile("src/test/resources/com/demonwav/mcdev/util/OuterClass.java") as PsiJavaFile).classes.single()
        this.outerAnonymousClass = outerClass.anonymousElements!!.single() as PsiAnonymousClass

        this.innerClass = outerClass.innerClasses.single()
        this.innerAnonymousClass = innerClass.anonymousElements!!.single() as PsiAnonymousClass
        this.innerAnonymousInnerClass = innerAnonymousClass.innerClasses.single()
    }

    fun testOuterInternalName() = assertEquals("com/example/test/OuterClass", outerClass.internalName)
    fun testOuterAnonymousInternalName() = assertEquals("com/example/test/OuterClass$1", outerAnonymousClass.internalName)
    fun testInnerInternalName() = assertEquals("com/example/test/OuterClass\$InnerClass", innerClass.internalName)
    fun testInnerAnonymousInternalName() =
            assertEquals("com/example/test/OuterClass\$InnerClass$1", innerAnonymousClass.internalName)
    fun testInnerAnonymousInnerInternalName() =
            assertEquals("com/example/test/OuterClass\$InnerClass$1\$AnonymousInnerClass", innerAnonymousInnerClass.internalName)

}
