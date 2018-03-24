/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.implements

import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import com.demonwav.mcdev.platform.mixin.inspection.implements.SoftImplementOverridesInspection
import org.intellij.lang.annotations.Language

class SoftImplementTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/DummyFace.java", """
                    package test;

                    interface DummyFace {

                        String thisMethodExists();

                    }
                """)

                java("test/SoftImplementMixin.java", fixPrefix("""
                    package test;

                    import org.spongepowered.asm.mixin.Mixin;
                    import org.spongepowered.asm.mixin.Implements;
                    import org.spongepowered.asm.mixin.Interface;

                    @Mixin
                    @Implements(@Interface(iface = DummyFace.class, prefix = "dummy_"))
                    class SoftImplementMixin {

                        public String dummy_thisMethodExists() {
                            return "test";
                        }

                        public int <error descr="Method does not soft-implement a method from its interfaces">dummy_thisMethodDoesntExist</error>() {
                            return 0;
                        }

                    }
                """))
            }
        }
    }

    private fun fixPrefix(@Language("JAVA") code: String): String {
        return code.replace('_', '$')
    }

    fun `test soft implements`() {
        myFixture.enableInspections(SoftImplementOverridesInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }
}
