/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.inspection.MixinSuperClassInspection
import org.intellij.lang.annotations.Language

class SuperClassTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/Entity.java", """
                    package test;

                    public class Entity {

                    }
                """)

                java("test/DemonWav.java", """
                    package test;

                    public class DemonWav extends Entity {

                    }
                """)

                java("test/Minecrell.java", """
                    package test;

                    public class Minecrell extends Entity {

                    }
                """)
            }
        }
    }

    private fun doTest(@Language("JAVA") mixinCode: String) {
        buildProject {
            src {
                java("test/SuperClassMixin.java", mixinCode)
            }
        }

        myFixture.enableInspections(MixinSuperClassInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test no mixin superclass`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(Minecrell.class)
            public class SuperClassMixin {

            }
        """)
    }


    fun `test good mixin superclass`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(Minecrell.class)
            public class SuperClassMixin extends Entity {

            }
        """)
    }

    fun `test mixin not its own superclass`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(DemonWav.class)
            public class SuperClassMixin extends <error descr="Cannot extend target class">DemonWav</error> {

            }
        """)
    }

    fun `test mixin class hierarchy not found`() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(DemonWav.class)
            public class SuperClassMixin extends <error descr="Cannot find 'Minecrell' in the hierarchy of target class 'DemonWav'">Minecrell</error> {

            }
        """)
    }

}
