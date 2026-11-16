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

package com.demonwav.mcdev.util

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.readLines
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Fails when main sources create an HTTP client or connection without going through [McdevHttp]. Requests that skip it
 * can't be redirected to the mock server, so tests would hit the real network.
 */
@DisplayName("HTTP Call Site Guard Tests")
class HttpCallSiteGuardTest {

    private class Rule(val name: String, val regex: Regex)

    private class Allowed(val pathPrefix: String, val rules: Set<String>?, val reason: String) {
        fun matches(path: String, rule: Rule) =
            (path == pathPrefix || path.startsWith(pathPrefix.trimEnd('/') + "/")) && (rules == null || rule.name in rules)
    }

    data class Violation(val path: String, val line: Int, val rule: String, val text: String) {
        override fun toString() = "$path:$line [$rule] $text"
    }

    private val rules = listOf(
        Rule("HttpRequests", Regex("""\bHttpRequests\s*\.\s*(request|post|put|patch|delete|head)\s*\(""")),
        // Ktor ships with the platform, so it's on the classpath even though the plugin doesn't use it
        Rule("Ktor HttpClient", Regex("""\bHttpClient\s*[({]""")),
        Rule("java.net.http HttpClient", Regex("""\bHttpClient\s*\.\s*new(HttpClient|Builder)\s*\(""")),
        Rule("HttpConnectionUtils", Regex("""\bHttpConnectionUtils\s*\.""")),
        Rule("openConnection", Regex("""\.openConnection\s*\(""")),
    )

    private val allowed = listOf(
        Allowed("util/McdevHttp.kt", null, "The routing helper itself."),
    )

    private fun scan(path: String, lines: List<String>, allowed: List<Allowed>): List<Violation> {
        val violations = mutableListOf<Violation>()
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("import ") || trimmed.startsWith("//") || trimmed.startsWith("*") ||
                trimmed.startsWith("/*")
            ) {
                continue
            }
            for (rule in rules) {
                if (rule.regex.containsMatchIn(line) && allowed.none { it.matches(path, rule) }) {
                    violations += Violation(path, index + 1, rule.name, trimmed)
                }
            }
        }
        return violations
    }

    private fun scanMainSources(allowed: List<Allowed>): List<Violation> {
        val root = Path.of("src", "main", "kotlin").toAbsolutePath()
        assertTrue(root.isDirectory(), "Main sources not found at $root, run tests from the project directory")
        return root.walk()
            .filter { it.extension == "kt" }
            .sortedBy { it.invariantSeparatorsPathString }
            .flatMap { file -> scan(file.relativeTo(root).invariantSeparatorsPathString, file.readLines(), allowed) }
            .toList()
    }

    @Test
    @DisplayName("Main sources only make HTTP requests through McdevHttp")
    fun noDirectHttpClients() {
        val violations = scanMainSources(allowed)
        assertTrue(violations.isEmpty()) {
            "Found HTTP clients created outside McdevHttp. Use McdevHttp so requests can be redirected to the test " +
                "mock server, or add an allowlist entry with a reason in HttpCallSiteGuardTest:\n" +
                violations.joinToString("\n")
        }
    }

    @Test
    @DisplayName("Every allowlist entry is still needed")
    fun allowlistIsUsed() {
        val unused = allowed.filter { entry ->
            scanMainSources(allowed - entry).none { violation ->
                val rule = rules.first { it.name == violation.rule }
                entry.matches(violation.path, rule)
            }
        }
        assertTrue(unused.isEmpty()) {
            "Allowlist entries no longer match anything, remove them:\n" +
                unused.joinToString("\n") { "${it.pathPrefix}: ${it.reason}" }
        }
    }
}
