/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

abstract class OuterClassTest : ProjectBuilderTest() {

    protected lateinit var outerClass: PsiClass
    protected lateinit var outerAnonymousClass: PsiAnonymousClass
    protected lateinit var innerClass: PsiClass
    protected lateinit var innerAnonymousClass: PsiAnonymousClass
    protected lateinit var innerAnonymousInnerClass: PsiClass
    protected lateinit var selfReferencingGeneric: PsiClass

    @BeforeEach
    fun setupProject() {
        buildProject {
            outerClass = java(
                "com/example/test/OuterClass.java",
                """
                package com.example.test;

                class OuterClass {

                    private static final Object ANONYMOUS_CLASS = new Object() {
                    };

                    class InnerClass {
                        public void test() {
                            new Object() {
                                class AnonymousInnerClass {

                                }
                            };
                        }
                    }

                    class SelfReferencingGeneric<C extends SelfReferencingGeneric<C>> {
                        public C doSomething() {}
                    }
                }
            """,
            ).toPsiFile<PsiJavaFile>().classes.single()
        }

        this.outerAnonymousClass = outerClass.anonymousElements.single() as PsiAnonymousClass

        this.innerClass = outerClass.innerClasses.first()
        this.innerAnonymousClass = innerClass.anonymousElements.single() as PsiAnonymousClass
        this.innerAnonymousInnerClass = innerAnonymousClass.innerClasses.single()

        this.selfReferencingGeneric = outerClass.innerClasses[1]
    }

    protected fun assertEquivalent(a: PsiElement, b: PsiElement?) {
        Assertions.assertTrue(a.manager.areElementsEquivalent(a, b), "Expected $a == $b")
    }
}
