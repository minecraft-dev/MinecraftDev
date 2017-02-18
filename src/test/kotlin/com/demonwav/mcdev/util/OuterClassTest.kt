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

import com.demonwav.mcdev.MinecraftCodeInsightFixtureTestCase
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile

abstract class OuterClassTest : MinecraftCodeInsightFixtureTestCase() {

    protected lateinit var outerClass: PsiClass
    protected lateinit var outerAnonymousClass: PsiAnonymousClass
    protected lateinit var innerClass: PsiClass
    protected lateinit var innerAnonymousClass: PsiAnonymousClass
    protected lateinit var innerAnonymousInnerClass: PsiClass

    override fun setUp() {
        super.setUp()

        this.outerClass = (buildProject {
            java("src/com/example/test/OuterClass.java", """
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
                }
            """)
        }.files.single() as PsiJavaFile).classes.single()

        this.outerAnonymousClass = outerClass.anonymousElements!!.single() as PsiAnonymousClass

        this.innerClass = outerClass.innerClasses.single()
        this.innerAnonymousClass = innerClass.anonymousElements!!.single() as PsiAnonymousClass
        this.innerAnonymousInnerClass = innerAnonymousClass.innerClasses.single()
    }
}
