/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.inspection.implements.DuplicateInterfaceInspection
import org.intellij.lang.annotations.Language

class DuplicateInterfaceInspectionTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                    }
                """)

                java("test/DummyFace2.java", """
                    package test;

                    interface DummyFace2 {

                    }
                """)
            }
        }
    }

    private fun doTest(@Language("JAVA") mixinCode: String) {
        buildProject {
            src {
                java("test/DuplicateInterfaceMixin.java", mixinCode)
            }
        }

        myFixture.enableInspections(DuplicateInterfaceInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }

    fun testGood() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements({
                @Interface(iface = DummyFace.class, prefix = "a$"),
                @Interface(iface = DummyFace2.class, prefix = "b$")
            })
            class DuplicateInterfaceMixin {

            }
        """)
    }

    fun testBad() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements({
                @Interface(iface = DummyFace.class, prefix = "a$"),
                <warning descr="Interface is already implemented">@Interface(iface = DummyFace.class, prefix = "b$")</warning>
            })
            class DuplicateInterfaceMixin {

            }
        """)
    }

}
