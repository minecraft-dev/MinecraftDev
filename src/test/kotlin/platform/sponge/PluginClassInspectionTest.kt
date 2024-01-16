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
            """,
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
            """,
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
            """,
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
            """,
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
            """,
        )
    }
}
