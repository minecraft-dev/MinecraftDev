/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Properties
import java.util.zip.ZipFile
import java.net.URL
import java.net.HttpURLConnection

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.code.gson:gson:2.8.6")
        classpath("com.github.salomonbrys.kotson:kotson:2.5.0")
    }
}

/*
 * This file and the task `resolveIntellijLibSources` below attempts to find sources for IntelliJ's dependencies (the
 * jars in its lib/ dir) which are in Maven Central. This check isn't perfect and may miss some, but grabs most of them.
 * The task writes the result to `.gradle/intellij-deps.json`.
 *
 * At configuration time if `.gradle/intellij-deps.json` exists the below code not in a task will add those dependencies
 * explicitly, causing Gradle to download the sources for those dependencies, and allowing IntelliJ to pick up those
 * sources. None of this affects the actual build or any of the code, this only exists to help ease development by
 * making library sources available in the dev environment.
 */

val fileName = ".gradle/intellij-deps.json"
val gson: Gson = GsonBuilder().setPrettyPrinting().create()

val jsonFile = file("$projectDir/$fileName")
if (jsonFile.exists()) {
    val deps = jsonFile.bufferedReader().use { reader ->
        gson.fromJson<List<Dep>>(reader)
    }
    dependencies {
        for ((groupId, artifactId, version) in deps) {
            "compileOnly"(
                group = groupId,
                name = artifactId,
                version = version
            )
        }
    }
}

data class Dep(val groupId: String, val artifactId: String, val version: String)

tasks.register("resolveIntellijLibSources") {
    group = "minecraft"
    val config = project.configurations["compileClasspath"]
    dependsOn(config)

    doLast {
        val files = config.resolvedConfiguration.files
        val deps = files.asSequence()
            .map { it.toPath() }
            .filter {
                it.map { part -> part.toString() }.containsAll(listOf("com.jetbrains.intellij.idea", "ideaIC", "lib"))
            }
            .filter { it.fileName.toString().endsWith(".jar") }
            .mapNotNull { lib ->
                val name = lib.fileName.toString()
                return@mapNotNull ZipFile(lib.toFile()).use { zipFile ->
                    val pomEntry = zipFile.stream()
                        .filter { entry ->
                            val entryName = entry.name
                            entryName.contains("META-INF/maven")
                                && entryName.split('/').any { name.contains(it) }
                                && entryName.endsWith("pom.properties")
                        }
                        .findFirst()
                        .orElse(null) ?: return@use null
                    return@use zipFile.getInputStream(pomEntry).use { input ->
                        val props = Properties()
                        props.load(input)
                        Dep(props["groupId"].toString(), props["artifactId"].toString(), props["version"].toString())
                    }
                }
            }.filter { dep ->
                // Check if this dependency is available in Maven Central
                val groupPath = dep.groupId.replace('.', '/')
                val (_, artifact, ver) = dep
                val url = "https://repo.maven.apache.org/maven2/$groupPath/$artifact/$ver/$artifact-$ver-sources.jar"
                return@filter with(URL(url).openConnection() as HttpURLConnection) {
                    try {
                        requestMethod = "GET"
                        val code = responseCode
                        return@with code in 200..299
                    } finally {
                        disconnect()
                    }
                }
            }.toList()

        jsonFile.bufferedWriter().use { writer ->
            gson.toJson(deps, writer)
        }
    }
}
