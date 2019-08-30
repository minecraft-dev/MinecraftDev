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
import com.demonwav.mcdev.platform.mixin.inspection.implements.DuplicateInterfacePrefixInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Duplicate Interface Prefix Inspection Tests")
class DuplicateInterfacePrefixInspectionTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
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
                java("test/DuplicateInterfacePrefixMixin.java", mixinCode)
            }
        }

        fixture.enableInspections(DuplicateInterfacePrefixInspection::class.java)
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("No Highlight On No Duplicate Interface Prefix Test")
    fun noHighlightOnNoDuplicateInterfacePrefixTest() {
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
            class DuplicateInterfacePrefixMixin {

            }
        """)
    }

    @Test
    @DisplayName("Highlight On Duplicate Interface Prefix Test")
    fun highlightOnDuplicateInterfacePrefixTest() {
        doTest("""
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements({
                @Interface(iface = DummyFace.class, prefix = "a$"),
                @Interface(iface = DummyFace2.class, prefix = <error descr="Duplicate prefix 'a$'">"a$"</error>)
            })
            class DuplicateInterfacePrefixMixin {

            }
        """)
    }
}
