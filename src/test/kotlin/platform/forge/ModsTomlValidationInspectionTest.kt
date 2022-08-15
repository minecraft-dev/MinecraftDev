/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.toml.platform.forge.inspections.ModsTomlValidationInspection
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Mods Toml Validation Inspection Tests")
class ModsTomlValidationInspectionTest : BasePlatformTestCase() {

    @BeforeEach
    override fun setUp() {
        super.setUp()
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
    }

    private fun doTest(@Language("TOML") text: String) {
        myFixture.configureByText("mods.toml", text)
        myFixture.enableInspections(ModsTomlValidationInspection::class.java)
        myFixture.checkHighlighting()
    }

    @Test
    @DisplayName("Invalid Mod ID")
    fun invalidModId() {
        doTest(
            """
[[mods]]
modId="<error descr="Mod ID is invalid">invalid id</error>"
            """
        )
    }

    @Test
    @DisplayName("Invalid Value Type")
    fun invalidValueType() {
        doTest(
            """
modLoader="javafml"
showAsResourcePack=<error descr="Wrong value type, expected boolean">"true"</error>
[[mods]]
modId="examplemod"
version=<error descr="Wrong value type, expected string">10</error>
logoBlur=<error descr="Wrong value type, expected boolean">"true"</error>
[[dependencies.examplemod]]
modId="forge"
versionRange=<error descr="Wrong value type, expected string">35</error>
mandatory=true
            """
        )
    }

    @Test
    @DisplayName("Invalid Enum Value")
    fun invalidEnumValue() {
        doTest(
            """
[[mods]]
modId="examplemod"
displayTest="<error descr="DisplayTest IGNORE_SERV does not exist">IGNORE_SERV</error>"
[[dependencies.examplemod]]
modId="forge"
ordering="<error descr="Order BEFO does not exist">BEFO</error>"
side="CLIENT"
modId="minecraft"
ordering="NONE"
side="<error descr="Side UP does not exist">UP</error>"
            """
        )
    }

    @Test
    @DisplayName("Dependency For Undeclared Mod")
    fun dependencyForUndeclaredMod() {
        doTest(
            """
[[mods]]
modId="examplemod1"
[[mods]]
modId="examplemod2"
[[dependencies.<error descr="Mod examplemod3 is not declared in this file">examplemod3</error>]]
            """
        )
    }
}
