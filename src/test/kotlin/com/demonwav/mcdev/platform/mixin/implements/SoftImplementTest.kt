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
import com.demonwav.mcdev.platform.mixin.inspection.implements.SoftImplementOverridesInspection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("@Implements Soft Implementation Inspection Test")
class SoftImplementTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                        String thisMethodExists();

                    }
                """)

                java("test/SoftImplementMixin.java", """
                    package test;

                    import org.spongepowered.asm.mixin.Mixin;
                    import org.spongepowered.asm.mixin.Implements;
                    import org.spongepowered.asm.mixin.Interface;

                    @Mixin
                    @Implements(@Interface(iface = DummyFace.class, prefix = "dummy$"))
                    class SoftImplementMixin {

                        public String dummy_thisMethodExists() {
                            return "test";
                        }

                        public int <error descr="Method does not soft-implement a method from its interfaces">dummy${'$'}thisMethodDoesntExist</error>() {
                            return 0;
                        }

                    }
                """)
            }
        }
    }

    @Test
    @DisplayName("Highlight Prefixed Method Not Implementing @Interface Test")
    fun highlightPrefixedMethodNotImplementingInterfaceTest() {
        fixture.enableInspections(SoftImplementOverridesInspection::class.java)
        fixture.checkHighlighting(true, false, false)
    }
}
