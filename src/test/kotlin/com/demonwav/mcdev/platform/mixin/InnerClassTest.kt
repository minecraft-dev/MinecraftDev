/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
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
            src {
                java("test/InnerClassMixin.java", """
                    package test;

                    import org.spongepowered.asm.mixin.Mixin;

                    @Mixin
                    class InnerClassMixin {

                        private static void test() {
                            new <error descr="Anonymous classes are not allowed in a @Mixin class">Object() {

                            }</error>;
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
                """)
            }
        }
    }

    @Test
    @DisplayName("Mixin Inner Class Inspection Test")
    fun mixinInnerClassInspectionTest() {
        fixture.enableInspections(MixinInnerClassInspection::class.java)
        fixture.checkHighlighting(true, false, false)
    }
}
