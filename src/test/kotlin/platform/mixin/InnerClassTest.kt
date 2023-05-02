/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.MixinInnerClassInspection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Mixin Inner Class Inspection Tests")
class InnerClassTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            dir("test") {
                java(
                    "InnerClassMixin.java",
                    """
                    package test;

                    import org.spongepowered.asm.mixin.Mixin;

                    @Mixin
                    class InnerClassMixin {

                        private static void anonymousClassOk() {
                            new Object() {
                            };
                        }

                        private static void stuffInsideAnonymousClassBad() {
                            new Object() {
                                public void foo() {
                                    new <error descr="Double nested anonymous classes are not allowed in a @Mixin class">Object() {
                                    }</error>;
                                }

                                <error descr="Inner class not allowed inside anonymous classes inside mixins">class ClassInsideAnonymousClass {
                                }</error>
                            };
                        }
                    
                        @Mixin
                        static class GoodInnerMixin {

                        }

                        <error descr="@Mixin inner class must be static">@Mixin
                        public</error> class NotStaticInnerMixin {

                        }

                        <error descr="Inner classes are only allowed if they are also @Mixin classes">class VeryBadInnerClass {

                        }</error>

                    }
                    """,
                )
            }
        }
    }

    @Test
    @DisplayName("Mixin Inner Class Inspection Test")
    fun mixinInnerClassInspectionTest() {
        fixture.enableInspections(MixinInnerClassInspection::class)
        fixture.checkHighlighting(true, false, false)
    }
}
