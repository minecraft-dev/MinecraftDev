/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.implements

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import com.demonwav.mcdev.platform.mixin.inspection.implements.InterfaceIsInterfaceInspection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("@Interface Is Interface Inspection Tests")
class InterfaceIsInterfaceTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            dir("test") {
                java(
                    "DummyFace.java",
                    """
                    package test;

                    interface DummyFace {

                    }
                    """,
                    configure = false
                )

                java(
                    "DummyClass.java",
                    """
                    package test;

                    class DummyClass {

                    }
                    """,
                    configure = false
                )

                java(
                    "InterfaceIsInterfaceMixin.java",
                    """
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
                    """
                )
            }
        }
    }

    @Test
    @DisplayName("Highlight On @Interface Test")
    fun highlightOnInterfaceTest() {
        fixture.enableInspections(InterfaceIsInterfaceInspection::class)
        fixture.checkHighlighting(true, false, false)
    }
}
