/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
            dir("test") {
                java(
                    "DummyFace.java",
                    """
                    package test;

                    interface DummyFace {

                    }
                    """,
                    configure = false,
                )
            }
        }
    }

    private fun doTest(@Language("JAVA") mixinCode: String) {
        buildProject {
            dir("test") {
                java("EmptyImplementsMixin.java", mixinCode)
            }
        }

        fixture.enableInspections(EmptyImplementsInspection::class)
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("Highlight On Empty @Implements Test")
    fun highlightOnEmptyImplementsTest() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            <warning descr="@Implements is redundant">@Implements({})</warning>
            class EmptyImplementsMixin {

            }
            """,
        )
    }

    @Test
    @DisplayName("No Highlight Wish Single @Implements Test")
    fun noHighlightWithSingleImplementsTest() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.Implements;
            import org.spongepowered.asm.mixin.Interface;

            @Mixin
            @Implements(@Interface(iface = DummyFace.class, prefix = "a$"))
            class EmptyImplementsMixin {

            }
            """,
        )
    }

    @Test
    @DisplayName("No Highlight With Multi @Implements Test")
    fun noHighlightWithMutliImplementsTest() {
        doTest(
            """
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
            """,
        )
    }
}
