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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType

class McPsiClassTest : OuterClassTest() {

    fun `test outer full qualified name`() = assertEquals("com.example.test.OuterClass", outerClass.fullQualifiedName)
    fun `test outer short name`() = assertEquals("OuterClass", outerClass.shortName)

    fun `test outer anonymous full qualified name`() = assertEquals("com.example.test.OuterClass$1", outerAnonymousClass.fullQualifiedName)
    fun `test outer anonymous short name`() = assertEquals("OuterClass.1", outerAnonymousClass.shortName)

    fun `test inner full qualified name`() = assertEquals("com.example.test.OuterClass\$InnerClass", innerClass.fullQualifiedName)
    fun `test inner short name`() = assertEquals("OuterClass.InnerClass", innerClass.shortName)

    fun `test inner anonymous full qualified name`() =
            assertEquals("com.example.test.OuterClass\$InnerClass$1", innerAnonymousClass.fullQualifiedName)
    fun `test inner anonymous short name`() = assertEquals("OuterClass.InnerClass.1", innerAnonymousClass.shortName)

    fun `test inner anonymous inner full qualified name`() =
            assertEquals("com.example.test.OuterClass\$InnerClass$1\$AnonymousInnerClass", innerAnonymousInnerClass.fullQualifiedName)
    fun `test inner anonymous inner short name`() = assertEquals("OuterClass.InnerClass.1.AnonymousInnerClass", innerAnonymousInnerClass.shortName)

    private fun findQualifiedClass(fullQualifiedName: String): PsiClass? {
        return findQualifiedClass(project, fullQualifiedName)
    }

    fun `test find outer`() = assertEquivalent(outerClass, findQualifiedClass("com.example.test.OuterClass"))
    fun `test find outerAnonymous`() = assertEquivalent(outerAnonymousClass, findQualifiedClass("com.example.test.OuterClass$1"))
    fun `test find inner`() = assertEquivalent(innerClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass"))
    fun `test find inner anonymous`() = assertEquivalent(innerAnonymousClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass$1"))
    fun `test find inner anonymous inner`() =
            assertEquivalent(innerAnonymousInnerClass, findQualifiedClass("com.example.test.OuterClass\$InnerClass$1\$AnonymousInnerClass"))

    fun `test self referencing generic`() = assertEquals("com.example.test.OuterClass\$SelfReferencingGeneric", selfReferencingGeneric.fullQualifiedName)
    fun `test self referencing generic method`() {
        assertNull((selfReferencingGeneric.methods.single().returnType as PsiClassType).fullQualifiedName)
    }

}
