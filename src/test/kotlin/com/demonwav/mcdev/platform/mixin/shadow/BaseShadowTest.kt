/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.shadow

import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import org.junit.jupiter.api.BeforeEach

abstract class BaseShadowTest : BaseMixinTest() {

    protected var mixins: ProjectBuilder.() -> Unit = {}
    protected abstract fun createMixins()

    @BeforeEach
    fun setupProject() {
        createMixins()
        buildProject {
            src {
                java("test/MixinBase.java", """
                    package test;

                    public class MixinBase {
                        // Static
                        private static final String privateStaticFinalString = "";
                        private static String privateStaticString = "";

                        protected static final String protectedStaticFinalString = "";
                        protected static String protectedStaticString = "";

                        static final String packagePrivateStaticFinalString = "";
                        static String packagePrivateStaticString = "";

                        public static final String publicStaticFinalString = "";
                        public static String publicStaticString = "";

                        // Non-static
                        private final String privateFinalString = "";
                        private String privateString = "";

                        protected final String protectedFinalString = "";
                        protected String protectedString = "";

                        final String packagePrivateFinalString = "";
                        String packagePrivateString = "";

                        public final String publicFinalString = "";
                        public String publicString = "";

                        // Bad shadows
                        protected String wrongAccessor = "";
                        protected final String noFinal = "";

                        public final String twoIssues = "";
                    }
                """)

                mixins()
            }
        }
    }
}
