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

@file:JvmName("HttpFixtureRecorder")

package com.demonwav.mcdev.framework.http.recorder

import com.demonwav.mcdev.framework.http.HttpFixtures
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Records fixtures from the live services. Run it through `./gradlew recordHttpFixtures`.
 *
 * Arguments: `<fixture root> [--templates <templates dir>] [--staging <dir>] [filter...]`.
 *
 * With no filters, every endpoint is recorded into the staging directory, which then replaces the fixture root. That
 * drops fixtures nothing requests anymore. If any endpoint fails, the root is left unchanged. The staging directory is
 * required in this mode, and must be on the same filesystem as the root.
 *
 * With filters, only endpoints whose URL contains one of them are written, straight into the root, and the rest of the
 * root is left alone. Endpoints recorded before a failure are kept. Endpoints that list other endpoints, such as
 * Parchment's version listing, are always fetched, so a filter can pick out the endpoints they list.
 */
fun main(args: Array<String>) {
    var root: Path? = null
    var templates: Path? = null
    var staging: Path? = null
    val filters = mutableListOf<String>()
    val iterator = args.iterator()
    while (iterator.hasNext()) {
        when (val arg = iterator.next()) {
            "--templates" -> templates = Paths.get(iterator.next())
            "--staging" -> staging = Paths.get(iterator.next())
            else -> if (root == null) root = Paths.get(arg) else filters += arg
        }
    }
    if (root == null || (filters.isEmpty() && staging == null)) {
        System.err.println(
            "Usage: HttpFixtureRecorder <fixture root> [--templates <dir>] [--staging <dir>] [filter...]\n" +
                "--staging is required when there are no filters",
        )
        exitProcess(2)
    }

    val recorder = FixtureRecorder(
        root.toAbsolutePath(),
        staging?.toAbsolutePath(),
        FixtureEndpoints.all(templates),
        filters,
    )
    exitProcess(if (recorder.run()) 0 else 1)
}

/**
 * Records [endpoints] into [root]. [staging] is required when [filters] is empty.
 */
class FixtureRecorder(
    private val root: Path,
    private val staging: Path?,
    private val endpoints: List<Endpoint>,
    private val filters: List<String>,
) {
    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val failures = mutableListOf<String>()
    private var saved = 0

    fun run(): Boolean {
        val target = if (filters.isEmpty()) {
            requireNotNull(staging) { "A full recording needs a staging directory" }.also {
                it.toFile().deleteRecursively()
            }
        } else {
            root
        }
        Files.createDirectories(target)

        for (endpoint in endpoints) {
            record(target, endpoint)
        }

        if (failures.isNotEmpty()) {
            System.err.println("\n${failures.size} endpoint(s) failed:")
            failures.forEach { System.err.println("  $it") }
            if (filters.isEmpty()) {
                System.err.println("The fixtures in $root were left unchanged.")
                target.toFile().deleteRecursively()
            }
            return false
        }
        if (saved == 0) {
            System.err.println("No endpoints match $filters")
            return false
        }

        if (filters.isEmpty()) {
            if (root.exists()) {
                root.toFile().deleteRecursively()
            }
            Files.move(target, root)
        }
        println("\nRecorded $saved fixture(s) into $root. Review the diff before committing.")
        return true
    }

    private fun matches(url: String): Boolean = filters.isEmpty() || filters.any { it in url }

    private fun record(target: Path, endpoint: Endpoint) {
        val wanted = matches(endpoint.url)
        val expand = endpoint.expand
        if (!wanted && expand == null) {
            return
        }

        val response = try {
            fetch(endpoint.url)
        } catch (e: Exception) {
            failures += "${endpoint.url}: $e"
            return
        }

        val status = response.statusCode()
        if (status !in endpoint.allowedStatuses) {
            failures += "${endpoint.url}: HTTP $status (final URL ${response.uri()})"
            return
        }

        val body = response.body()
        if (wanted && !save(target, endpoint, response)) {
            return
        }

        if (status == 200 && expand != null) {
            for (child in expand(body)) {
                record(target, child)
            }
        }
    }

    /**
     * Returns false if the response couldn't be saved, after recording why.
     */
    private fun save(target: Path, endpoint: Endpoint, response: HttpResponse<ByteArray>): Boolean {
        val status = response.statusCode()
        val body = response.body()
        val path = HttpFixtures.mappingFileName(endpoint.url)
        if (status == 200) {
            val trimmed = try {
                endpoint.trim.apply(body)
            } catch (e: Exception) {
                failures += "${endpoint.url}: failed to trim the response, has the format changed? $e"
                return false
            }
            val headers = response.headers().firstValue("Content-Type")
                .map { mapOf("Content-Type" to it) }
                .orElse(emptyMap())
            HttpFixtures.write(target, endpoint.url, status, headers, trimmed)
            println("$status ${endpoint.url}\n    -> $path (${body.size} bytes, saved ${trimmed.size})")
        } else {
            // Error pages aren't worth keeping, the status is what the creator reacts to
            HttpFixtures.write(target, endpoint.url, status, emptyMap(), null)
            println("$status ${endpoint.url}\n    -> $path")
        }
        saved++
        return true
    }

    private fun fetch(url: String): HttpResponse<ByteArray> {
        val request = HttpRequest.newBuilder(URI(url))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build()

        var last: Exception? = null
        repeat(ATTEMPTS) { attempt ->
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                if (response.statusCode() != 429 && response.statusCode() < 500) {
                    return response
                }
                last = IOException("HTTP ${response.statusCode()}")
            } catch (e: IOException) {
                last = e
            }
            Thread.sleep(1000L * (attempt + 1))
        }
        throw last!!
    }

    companion object {
        private const val ATTEMPTS = 3
        private const val USER_AGENT = "minecraft-dev-fixture-recorder (https://github.com/minecraft-dev/MinecraftDev)"
    }
}
