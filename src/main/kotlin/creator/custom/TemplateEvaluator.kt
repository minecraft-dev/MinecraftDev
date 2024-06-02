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

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.util.StringBuilderWriter

object TemplateEvaluator {

    val baseProperties = mapOf(
        "semver" to SemanticVersion.Companion,
        "mcver" to MinecraftVersions
    )

    fun evaluate(properties: Map<String, Any?>, template: String): Result<Pair<Boolean, String>> {
        val context = VelocityContext(baseProperties + properties)
        val stringWriter = StringBuilderWriter()
        return runCatching {
            Velocity.evaluate(context, stringWriter, "McDevTplExpr", template) to stringWriter.toString()
        }
    }

    fun template(properties: Map<String, Any?>, template: String): Result<String> {
        return evaluate(properties, template).map { it.second }
    }

    fun condition(properties: Map<String, Any?>, condition: String): Result<Boolean> {
        val actualCondition = "#if ($condition) true #else false #end"
        return evaluate(properties, actualCondition).map { it.second.trim().toBoolean() }
    }
}
