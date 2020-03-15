/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.implements

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import com.demonwav.mcdev.platform.mixin.inspection.implements.InterfacePrefixInspection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("@Interface Prefix Format Inspection Tests")
class InterfacePrefixTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
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

    @Test
    @DisplayName("Highlight On @Interface With Bad Prefix Test")
    fun highlightOnInterfaceWithBadPrefixTest() {
        fixture.enableInspections(InterfacePrefixInspection::class.java)
        fixture.checkHighlighting(true, false, false)
    }
}
