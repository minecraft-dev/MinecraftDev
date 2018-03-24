/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.implements

import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import com.demonwav.mcdev.platform.mixin.inspection.implements.EmptyImplementsInspection
import org.intellij.lang.annotations.Language

class EmptyImplementsTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                    }
                """)
            }
        }
    }

    private fun doTest(@Language("JAVA") mixinCode: String) {
        buildProject {
            src {
                java("test/EmptyImplementsMixin.java", mixinCode)
            }
        }

        myFixture.enableInspections(EmptyImplementsInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test highlight on empty @Implements`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            <warning descr="@Implements is redundant">@Implements({})</warning>
            class EmptyImplementsMixin {

            }
        """)
    }

    fun `test no highlight with single @Implements`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements(@Interface(iface = DummyFace.class, prefix = "a$"))
            class EmptyImplementsMixin {

            }
        """)
    }

    fun `test no highlight with mutli @Implements`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements({
                @Interface(iface = DummyFace.class, prefix = "a$"),
                @Interface(iface = DummyFace.class, prefix = "b$"),
            })
            class EmptyImplementsMixin {

            }
        """)
    }
}
