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
import com.demonwav.mcdev.platform.mixin.inspection.implements.DuplicateInterfaceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Duplicate Interface Inspection Tests")
class DuplicateInterfaceInspectionTest : BaseMixinTest() {

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

                java(
                    "DummyFace2.java",
                    """
                    package test;

                    interface DummyFace2 {

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
                java("DuplicateInterfaceMixin.java", mixinCode)
            }
        }

        fixture.enableInspections(DuplicateInterfaceInspection::class)
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("No Highlight On No Duplicate Interface Test")
    fun noHighlightOnNoDuplicateInterfaceTest() {
        doTest(
            """
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
            """,
        )
    }

    @Test
    @DisplayName("Highlight On Duplicate Interface Test")
    fun highlightOnDuplicateInterfaceTest() {
        doTest(
            """
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
            """,
        )
    }
}
