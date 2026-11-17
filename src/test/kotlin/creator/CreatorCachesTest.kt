/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.creator.custom.CreatorCaches
import com.demonwav.mcdev.creator.custom.providers.RemoteTemplateProvider.RemoteAuthType
import com.demonwav.mcdev.creator.custom.providers.RemoteTemplateProvider.RemoteTemplateRepo
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.framework.http.HttpMockExtension
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.testFramework.fixtures.BareTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@DisplayName("Creator Caches Tests")
class CreatorCachesTest {

    @JvmField
    @RegisterExtension
    val http = HttpMockExtension()

    private lateinit var fixture: BareTestFixture

    private val builtinZipUrl = "https://github.com/minecraft-dev/templates/archive/refs/heads/v4.zip"

    @BeforeEach
    fun setUp() {
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        fixture.setUp()
        CreatorCaches.clear()
    }

    @AfterEach
    fun tearDown() {
        try {
            CreatorCaches.clear()
        } finally {
            fixture.tearDown()
        }
    }

    private fun savedZips(): List<String> {
        val dir = RemoteTemplateRepo.templatesBaseDir
        if (!dir.exists()) {
            return emptyList()
        }
        return Files.list(dir).use { files ->
            files.filter { it.isRegularFile() }.map { it.fileName.toString() }.toList()
        }
    }

    private fun init(providerKey: String, repo: MinecraftSettings.TemplateRepo) = runBlocking {
        TemplateProvider.get(providerKey)!!.init(EmptyProgressIndicator(), listOf(repo))
    }

    @Test
    @DisplayName("The builtin provider updates again after a reset, and the saved zip is deleted")
    fun builtinProvider() {
        val repo = MinecraftSettings.TemplateRepo.makeBuiltinRepo()

        init("builtin", repo)
        init("builtin", repo)
        assertEquals(1, http.server.requestsTo(builtinZipUrl).size, "The second init should reuse the update")
        assertTrue(savedZips().isNotEmpty())

        CreatorCaches.clear()
        assertEquals(emptyList<String>(), savedZips())

        init("builtin", repo)
        assertEquals(2, http.server.requestsTo(builtinZipUrl).size)
    }

    @Test
    @DisplayName("The remote provider updates again after a reset, and the saved zip is deleted")
    fun remoteProvider() {
        val data = RemoteTemplateRepo(builtinZipUrl, true, "", RemoteAuthType.NONE, "").serialize()
        val repo = MinecraftSettings.TemplateRepo("Cache test", "remote", data)

        init("remote", repo)
        init("remote", repo)
        assertEquals(1, http.server.requestsTo(builtinZipUrl).size, "The second init should reuse the update")
        assertEquals(listOf("Cache test.zip"), savedZips())

        CreatorCaches.clear()
        assertFalse(RemoteTemplateRepo.getDestinationZip("Cache test").exists())

        init("remote", repo)
        assertEquals(2, http.server.requestsTo(builtinZipUrl).size)
    }
}
