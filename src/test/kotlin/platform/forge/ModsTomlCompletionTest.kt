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

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Mods Toml Completion Tests")
class ModsTomlCompletionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resources/com/demonwav/mcdev/platform/forge/modsTomlCompletion"
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    @DisplayName("Root Keys")
    fun rootKeys() {
        myFixture.testCompletionVariants(
            "rootKeys/mods.toml",
            "modLoader",
            "loaderVersion",
            "license",
            "showAsResourcePack",
            "issueTrackerURL"
        )
    }

    @Test
    @DisplayName("Mods Keys")
    fun modsKeys() {
        myFixture.testCompletionVariants(
            "modsKeys/mods.toml",
            "modId",
            "version",
            "displayName",
            "updateJSONURL",
            "displayURL",
            "logoFile",
            "logoBlur",
            "credits",
            "authors",
            "displayTest",
            "description"
        )
    }

    @Test
    @DisplayName("Dependencies Keys")
    fun dependenciesKeys() {
        myFixture.testCompletionVariants(
            "dependenciesKeys/mods.toml",
            "modId",
            "mandatory",
            "versionRange",
            "ordering",
            "side"
        )
    }

    @Test
    @DisplayName("Mod Dependency Key")
    fun modDependencyKey() {
        myFixture.testCompletionVariants("modDependencyKey/mods.toml", "declared_mod_1", "declared_mod_2")
    }

    @Test
    @DisplayName("Boolean Value")
    fun booleanValue() {
        myFixture.testCompletionVariants("boolean/mods.toml", "true", "false")
    }

    @Test
    @DisplayName("Display Test Value")
    fun displayTestValue() {
        myFixture.testCompletionVariants(
            "displayTestValue/mods.toml",
            "MATCH_VERSION",
            "IGNORE_SERVER_VERSION",
            "IGNORE_ALL_VERSION",
            "NONE"
        )
    }

    @Test
    @DisplayName("Dependency Ordering Value")
    fun orderingValue() {
        myFixture.testCompletionVariants("dependencyOrderingValue/mods.toml", "NONE", "BEFORE", "AFTER")
    }

    @Test
    @DisplayName("Dependency Side Value")
    fun sideValue() {
        myFixture.testCompletionVariants("dependencySideValue/mods.toml", "BOTH", "CLIENT", "SERVER")
    }

    @Test
    @DisplayName("String Completion From Nothing")
    fun stringCompletionFromNothing() {
        myFixture.testCompletion(
            "stringCompletionFromNothing/mods.toml",
            "stringCompletionFromNothing/mods.toml.after"
        )
    }

    @Test
    @DisplayName("String Completion")
    fun stringCompletion() {
        myFixture.testCompletion(
            "stringCompletion/mods.toml",
            "stringCompletion/mods.toml.after"
        )
    }
}
