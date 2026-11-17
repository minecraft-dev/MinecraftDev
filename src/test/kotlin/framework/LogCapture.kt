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

package com.demonwav.mcdev.framework

import com.intellij.testFramework.LoggedErrorProcessor
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Collects the errors and warnings logged from any thread until closed. While open, logged errors don't fail the test
 * on their own, so check [errors] or call [assertNoErrors].
 */
class LogCapture : AutoCloseable {

    val errors: MutableList<String> = CopyOnWriteArrayList()
    val warnings: MutableList<String> = CopyOnWriteArrayList()

    private val token = LoggedErrorProcessor.executeWith(
        object : LoggedErrorProcessor() {
            override fun processError(
                category: String,
                message: String,
                details: Array<out String>,
                t: Throwable?,
            ): Set<Action> {
                errors += describe(category, message, t)
                return Action.NONE
            }

            override fun processWarn(category: String, message: String, t: Throwable?): Boolean {
                warnings += describe(category, message, t)
                return true
            }
        },
    )

    fun assertNoErrors() {
        if (errors.isNotEmpty()) {
            throw AssertionError("Errors were logged:\n" + errors.joinToString("\n") { "  $it" })
        }
    }

    override fun close() {
        token.finish()
    }

    private fun describe(category: String, message: String, t: Throwable?): String =
        if (t == null) "[$category] $message" else "[$category] $message: $t"
}
