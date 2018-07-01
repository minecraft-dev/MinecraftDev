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

import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile

abstract class OuterClassTest : ProjectBuilderTest() {

    protected lateinit var outerClass: PsiClass
    protected lateinit var outerAnonymousClass: PsiAnonymousClass
    protected lateinit var innerClass: PsiClass
    protected lateinit var innerAnonymousClass: PsiAnonymousClass
    protected lateinit var innerAnonymousInnerClass: PsiClass
    protected lateinit var selfReferencingGeneric: PsiClass

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                outerClass = java("com/example/test/OuterClass.java", """
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
                """).toPsiFile<PsiJavaFile>().classes.single()
            }
        }

        this.outerAnonymousClass = outerClass.anonymousElements.single() as PsiAnonymousClass

        this.innerClass = outerClass.innerClasses.first()
        this.innerAnonymousClass = innerClass.anonymousElements.single() as PsiAnonymousClass
        this.innerAnonymousInnerClass = innerAnonymousClass.innerClasses.single()

        this.selfReferencingGeneric = outerClass.innerClasses[1]
    }

    protected fun assertEquivalent(a: PsiElement, b: PsiElement?) {
        assertTrue("Expected $a == $b", a.manager.areElementsEquivalent(a, b))
    }
}
