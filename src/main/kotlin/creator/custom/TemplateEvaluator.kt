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
