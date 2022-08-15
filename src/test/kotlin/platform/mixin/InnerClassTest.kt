/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
                    """
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
