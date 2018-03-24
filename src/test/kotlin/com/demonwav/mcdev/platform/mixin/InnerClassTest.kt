/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.inspection.MixinInnerClassInspection

class InnerClassTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

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

    fun `test inner class inspection`() {
        myFixture.enableInspections(MixinInnerClassInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }
}
