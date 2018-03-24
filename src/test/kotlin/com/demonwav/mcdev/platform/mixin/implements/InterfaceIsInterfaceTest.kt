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
import com.demonwav.mcdev.platform.mixin.inspection.implements.InterfaceIsInterfaceInspection

class InterfaceIsInterfaceTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                    }
                """)

                java("test/DummyClass.java", """
                    package test;

                    class DummyClass {

                    }
                """)

                java("test/InterfaceIsInterfaceMixin.java", """
                    package test;

                    import org.spongepowered.asm.mixin.Mixin;
                    import org.spongepowered.asm.mixin.Implements;
                    import org.spongepowered.asm.mixin.Interface;

                    @Mixin
                    @Implements({
                        @Interface(iface = DummyFace.class, prefix = "good$"),
                        @Interface(iface = <error descr="Interface expected here">DummyClass.class</error>, prefix = "bad$"),
                    })
                    class InterfaceIsInterfaceMixin {

                    }
                """)
            }
        }
    }

    fun `test if @Interface is interface`() {
        myFixture.enableInspections(InterfaceIsInterfaceInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }
}
