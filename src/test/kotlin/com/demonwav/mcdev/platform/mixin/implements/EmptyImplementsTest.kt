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
import com.demonwav.mcdev.platform.mixin.inspection.implements.EmptyImplementsInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Empty Implements Inspection Tests")
class EmptyImplementsTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
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

        fixture.enableInspections(EmptyImplementsInspection::class.java)
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("Highlight On Empty @Implements Test")
    fun highlightOnEmptyImplementsTest() {
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

    @Test
    @DisplayName("No Highlight Wish Single @Implements Test")
    fun noHighlightWithSingleImplementsTest() {
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

    @Test
    @DisplayName("No Highlight With Multi @Implements Test")
    fun noHighlightWithMutliImplementsTest() {
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
