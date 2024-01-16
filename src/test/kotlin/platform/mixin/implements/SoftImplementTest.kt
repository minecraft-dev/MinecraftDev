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
            dir("test") {
                java(
                    "DummyFace.java",
                    """
                    package test;

                    interface DummyFace {

                        String thisMethodExists();

                    }
                    """,
                    configure = false,
                )

                java(
                    "SoftImplementMixin.java",
                    """
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
                    """,
                )
            }
        }
    }

    @Test
    @DisplayName("Highlight Prefixed Method Not Implementing @Interface Test")
    fun highlightPrefixedMethodNotImplementingInterfaceTest() {
        fixture.enableInspections(SoftImplementOverridesInspection::class)
        fixture.checkHighlighting(true, false, false)
    }
}
