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

class McPsiClassTest : LightCodeInsightFixtureTestCase() {

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

    fun testOuterFullQualifiedName() = assertEquals("com.example.test.OuterClass", outerClass.fullQualifiedName)
    fun testOuterShortName() = assertEquals("OuterClass", outerClass.shortName)

    fun testOuterAnonymousFullQualifiedName() = assertEquals("com.example.test.OuterClass$1", outerAnonymousClass.fullQualifiedName)
    fun testOuterAnonymousShortName() = assertEquals("OuterClass.1", outerAnonymousClass.shortName)

    fun testInnerFullQualifiedName() = assertEquals("com.example.test.OuterClass\$InnerClass", innerClass.fullQualifiedName)
    fun testInnerShortName() = assertEquals("OuterClass.InnerClass", innerClass.shortName)

    fun testInnerAnonymousFullQualifiedName() =
            assertEquals("com.example.test.OuterClass\$InnerClass$1", innerAnonymousClass.fullQualifiedName)
    fun testInnerAnonymousShortName() = assertEquals("OuterClass.InnerClass.1", innerAnonymousClass.shortName)

    fun testInnerAnonymousInnerFullQualifiedName() =
            assertEquals("com.example.test.OuterClass\$InnerClass$1\$AnonymousInnerClass", innerAnonymousInnerClass.fullQualifiedName)
    fun testInnerAnonymousInnerShortName() = assertEquals("OuterClass.InnerClass.1.AnonymousInnerClass", innerAnonymousInnerClass.shortName)

    private fun findQualifiedClass(fullQualifiedName: String): PsiClass? {
        return findQualifiedClass(project, fullQualifiedName)
    }

    fun testFindOuter() = assertSame(outerClass, findQualifiedClass("com.example.test.OuterClass"))
    fun testFindOuterAnonymous() = assertSame(outerAnonymousClass, findQualifiedClass("com.example.test.OuterClass$1"))
    fun testFindInner() = assertSame(innerClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass"))
    fun testFindInnerAnonymous() = assertSame(innerAnonymousClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass$1"))
    fun testFindInnerAnonymousInner() =
            assertSame(innerAnonymousInnerClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass$1\$AnonymousInnerClass"))

}
