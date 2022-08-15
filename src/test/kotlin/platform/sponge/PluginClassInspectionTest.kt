/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.sponge.inspection.SpongePluginClassInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Sponge Plugin Class Inspection Tests")
class PluginClassInspectionTest : BaseSpongeTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("ASpongePlugin.java", code)
            }
        }

        fixture.enableInspections(SpongePluginClassInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Invalid Plugin Id Test")
    fun invalidPluginIdTest() {
        doTest(
            """
            package test;
            
            import org.spongepowered.api.plugin.Plugin;
            
            @Plugin(id = <error descr="Plugin IDs should be lowercase, and only contain characters from a-z, dashes or underscores, start with a lowercase letter, and not exceed 64 characters.">"a plugin"</error>)
            public class ASpongePlugin {
                ASpongePlugin() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Default Constructor Test")
    fun defaultConstructorTest() {
        doTest(
            """
            package test;
            
            import org.spongepowered.api.plugin.Plugin;
            
            @Plugin(id = "a-plugin")
            public class ASpongePlugin {
            }
            """
        )
    }

    @Test
    @DisplayName("Private Constructor Test")
    fun privateConstructorTest() {
        doTest(
            """
            package test;
            
            import org.spongepowered.api.plugin.Plugin;
            
            @Plugin(id = "a-plugin")
            public class ASpongePlugin {
                private <error descr="Plugin class empty constructor must not be private.">ASpongePlugin</error>() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Private Constructor With Injected Constructor Test")
    fun privateConstructorWithInjectedConstructorTest() {
        doTest(
            """
            package test;
            
            import com.google.inject.Inject;
            import org.slf4j.Logger;
            import org.spongepowered.api.plugin.Plugin;
            
            @Plugin(id = "a-plugin")
            public class ASpongePlugin {
                private ASpongePlugin() {
                }
            
                @Inject
                private ASpongePlugin(Logger logger) {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("No Private Constructor With Injected Constructor Test")
    fun noPrivateConstructorWithInjectedConstructorTest() {
        doTest(
            """
            package test;
            
            import com.google.inject.Inject;
            import org.slf4j.Logger;
            import org.spongepowered.api.plugin.Plugin;
            
            @Plugin(id = "a-plugin")
            public class ASpongePlugin {
                @Inject
                private ASpongePlugin(Logger logger) {
                }
            }
            """
        )
    }
}
