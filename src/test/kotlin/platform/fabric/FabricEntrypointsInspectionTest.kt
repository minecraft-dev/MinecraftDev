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

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.platform.fabric.inspection.FabricEntrypointsInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Fabric Entrypoints Inspection Tests")
class FabricEntrypointsInspectionTest : BaseFabricTest() {

    private fun doTest(@Language("JSON") json: String, builder: (ProjectBuilder.() -> Unit) = {}) {
        buildProject {
            java(
                "GoodSimpleModInitializer.java",
                """
                import net.fabricmc.api.ModInitializer;

                public class GoodSimpleModInitializer implements ModInitializer {
                    @Override
                    public void onInitialize() {
                    }
                    
                    public void handle() {}
                }
                """.trimIndent()
            )
            java(
                "GoodSimpleClientModInitializer.java",
                """
                import net.fabricmc.api.ClientModInitializer;

                public class GoodSimpleClientModInitializer implements ClientModInitializer {
                    @Override
                    public void onInitializeClient() {
                    }
                }
                """.trimIndent()
            )
            java(
                "BadSimpleModInitializer.java",
                """
                public class BadSimpleModInitializer {
                    public void handle(String param) {}
                }
                """.trimIndent()
            )
            java(
                "BadSimpleClientModInitializer.java",
                """
                public class BadSimpleClientModInitializer {}
                """.trimIndent()
            )

            builder()

            json("fabric.mod.json", json)
        }

        fixture.enableInspections(FabricEntrypointsInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    fun validInitializers() {
        doTest(
            """
            {
              "entrypoints": {
                "main": [
                  {
                    "value": "GoodSimpleModInitializer"
                  },
                  "GoodSimpleModInitializer::handle"
                ],
                "client": [
                  "GoodSimpleClientModInitializer"
                ]
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun invalidInitializers() {
        doTest(
            """
            {
              "entrypoints": {
                "main": [
                  "<error descr="'main' entrypoints must implement net.fabricmc.api.ModInitializer">GoodSimpleClientModInitializer</error>",
                  {
                    "value": "<error descr="'main' entrypoints must implement net.fabricmc.api.ModInitializer">BadSimpleModInitializer</error>"
                  }
                ],
                "client": [
                  "<error descr="'client' entrypoints must implement net.fabricmc.api.ClientModInitializer">BadSimpleClientModInitializer</error>",
                  "<error descr="'client' entrypoints must implement net.fabricmc.api.ClientModInitializer">GoodSimpleModInitializer</error>"
                ]
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun missingEmptyConstructor() {
        doTest(
            """
            {
              "entrypoints": {
                "main": [
                  "<error descr="Entrypoint class must have an empty constructor">BadCtorSimpleModInitializer</error>"
                ]
              }
            }
            """.trimIndent()
        ) {
            java(
                "BadCtorSimpleModInitializer.java",
                """
                import net.fabricmc.api.ModInitializer;
                
                public class BadCtorSimpleModInitializer implements ModInitializer {
                    public BadCtorSimpleModInitializer(String param) {}
                }
                """.trimIndent()
            )
        }
    }

    @Test
    fun entrypointMethodWithParameter() {
        doTest(
            """
            {
              "entrypoints": {
                "main": [
                  "BadSimpleModInitializer::<error descr="Entrypoint method must have no parameters">handle</error>"
                ]
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun entrypointInstanceMethodInClassWithNoEmptyCtor() {
        doTest(
            """
            {
              "entrypoints": {
                "main": [
                  "BadTestInitializer::goodInitialize",
                  "BadTestInitializer::<error descr="Entrypoint instance method class must have an empty constructor">badInitialize</error>"
                ]
              }
            }
            """.trimIndent()
        ) {
            java(
                "BadTestInitializer.java",
                """
                public class BadTestInitializer {
                    public BadTestInitializer(String param) {}
                    public static void goodInitialize() {}
                    public void badInitialize() {}
                }
                """.trimIndent()
            )
        }
    }

    @Test
    fun entrypointFieldInitializers() {
        doTest(
            """
            {
              "entrypoints": {
                "main": [
                  "ModInitializerContainer::initializer",
                  "ModInitializerContainer::<error descr="'main' entrypoints must be of type net.fabricmc.api.ModInitializer">badTypeInitializer</error>"
                ]
              }
            }
            """.trimIndent()
        ) {
            java(
                "ModInitializerContainer.java",
                """
                public class ModInitializerContainer {
                    public static GoodSimpleModInitializer initializer = new GoodSimpleModInitializer();
                    public static String badTypeInitializer = "No...";
                }
                """.trimIndent()
            )
        }
    }
}
