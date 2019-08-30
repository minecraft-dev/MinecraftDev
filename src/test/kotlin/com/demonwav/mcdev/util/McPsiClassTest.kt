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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("class-util Extension Tests")
class McPsiClassTest : OuterClassTest() {

    @Test
    @DisplayName("fullQualifiedName Of Outer Class Test")
    fun outerFullQualifiedNameTest() =
        Assertions.assertEquals("com.example.test.OuterClass", outerClass.fullQualifiedName)
    @Test
    @DisplayName("shortName Of Outer Class Test")
    fun outerShortNameTest() = Assertions.assertEquals("OuterClass", outerClass.shortName)

    @Test
    @DisplayName("fullQualifiedName of Outer Anonymous Class Test")
    fun outerAnonymousFullQualifiedNameTest() =
        Assertions.assertEquals("com.example.test.OuterClass$1", outerAnonymousClass.fullQualifiedName)
    @Test
    @DisplayName("shortName Of Outer Anonymous Class Test")
    fun outerAnonymousShortNameTest() = Assertions.assertEquals("OuterClass.1", outerAnonymousClass.shortName)

    @Test
    @DisplayName("fullQualifiedName Of Inner Class Test")
    fun innerFullQualifiedNameTest() =
        Assertions.assertEquals("com.example.test.OuterClass\$InnerClass", innerClass.fullQualifiedName)
    @Test
    @DisplayName("shortName Of Inner Class Test")
    fun innerShortNameTest() = Assertions.assertEquals("OuterClass.InnerClass", innerClass.shortName)

    @Test
    @DisplayName("fullQualifiedName Of Inner Anonymous Class Test")
    fun innerAnonymousFullQualifiedNameTest() =
        Assertions.assertEquals("com.example.test.OuterClass\$InnerClass$1", innerAnonymousClass.fullQualifiedName)
    @Test
    @DisplayName("shortName Of Inner Anonymous Class Test")
    fun innerAnonymousShortNameTest() =
        Assertions.assertEquals("OuterClass.InnerClass.1", innerAnonymousClass.shortName)

    @Test
    @DisplayName("fullQualifiedName Of Inner Anonymous Inner Class Test")
    fun innerAnonymousInnerFullQualifiedNameTest() =
        Assertions.assertEquals(
                "com.example.test.OuterClass\$InnerClass$1\$AnonymousInnerClass",
                innerAnonymousInnerClass.fullQualifiedName
            )
    @Test
    @DisplayName("shortName Of Inner Anonymous Inner Class Test")
    fun innerAnonymousInnerShortNameTest() =
        Assertions.assertEquals("OuterClass.InnerClass.1.AnonymousInnerClass", innerAnonymousInnerClass.shortName)

    private fun findQualifiedClass(fullQualifiedName: String): PsiClass? {
        return findQualifiedClass(project, fullQualifiedName)
    }

    @Test
    @DisplayName("findQualifiedClass Of Outer Class Test")
    fun outerFindTest() =
        assertEquivalent(outerClass, findQualifiedClass("com.example.test.OuterClass"))
    @Test
    @DisplayName("findQualifiedClass Of Outer Anonymous Class Test")
    fun outerAnonymousFindTest() =
        assertEquivalent(outerAnonymousClass, findQualifiedClass("com.example.test.OuterClass$1"))
    @Test
    @DisplayName("findQualifiedClass Of Inner Class Test")
    fun innerFindTest() =
        assertEquivalent(innerClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass"))
    @Test
    @DisplayName("findQualifiedClass Of Inner Anonymous Class Test")
    fun innerAnonymousFindTest() =
        assertEquivalent(
            innerAnonymousClass,
            findQualifiedClass(
            "com.example.test.OuterClass\$InnerClass$1"
            )
        )
    @Test
    @DisplayName("findQualifiedClass Of Inner Anonymous Inner Class Test")
    fun innerAnonymousInnerFindTest() =
            assertEquivalent(
                innerAnonymousInnerClass,
                findQualifiedClass("com.example.test.OuterClass\$InnerClass$1\$AnonymousInnerClass")
            )

    @Test
    @DisplayName("fullQualifiedName Of Self-Referencing Generic Class Test")
    fun selfReferencingGenericFullQualifiedNameTest() =
        Assertions.assertEquals(
            "com.example.test.OuterClass\$SelfReferencingGeneric",
            selfReferencingGeneric.fullQualifiedName
        )
    @Test
    @DisplayName("fullQualifiedName Of Self-Referencing Generic Method Test")
    fun selfReferencingGenericFullQualifiedNameMethodTest() {
        Assertions.assertNull((selfReferencingGeneric.methods.single().returnType as PsiClassType).fullQualifiedName)
    }
}
