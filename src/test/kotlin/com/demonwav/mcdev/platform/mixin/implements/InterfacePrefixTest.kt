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
import com.demonwav.mcdev.platform.mixin.inspection.implements.InterfacePrefixInspection

class InterfacePrefixTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                    }
                """)

                java("test/InterfacePrefixMixin.java", """
                    package test;

                    import org.spongepowered.asm.mixin.Mixin;
                    import org.spongepowered.asm.mixin.Implements;
                    import org.spongepowered.asm.mixin.Interface;

                    @Mixin
                    @Implements({
                        @Interface(iface = DummyFace.class, prefix = "good$"),
                        @Interface(iface = DummyFace.class, prefix = <error descr="@Interface prefix must end with a dollar sign ($)">"bad"</error>),
                    })
                    class InterfacePrefixMixin {

                    }
                """)
            }
        }
    }

    fun `test interface prefix inspection`() {
        myFixture.enableInspections(InterfacePrefixInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }
}
