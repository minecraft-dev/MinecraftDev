/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import java.util.zip.ZipFile

val jflex: Configuration by configurations.creating
val jflexSkeleton: Configuration by configurations.creating
val grammarKit: Configuration by configurations.creating
val compileOnly by configurations

// Analyze dependencies
val fileName = ".gradle/intellij-deps.json"
val jsonFile = file("$projectDir/$fileName")

val ideaVersion: String by project
val ideaVersionName: String by project

if (jsonFile.exists()) {
    val deps: DepList = jsonFile.bufferedReader().use { reader ->
        Gson().fromJson(reader, DepList::class.java)
    }
    if (ideaVersion != deps.intellijVersion || ideaVersionName != deps.intellijVersionName) {
        println("IntelliJ library sources file definition is out of date, deleting")
        jsonFile.delete()
    } else {
        dependencies {
            for ((groupId, artifactId, version) in deps.deps) {
                compileOnly(
                    group = groupId,
                    name = artifactId,
                    version = version
                )
            }
        }
    }
}

tasks.register("resolveIntellijLibSources") {
    group = "minecraft"
    val compileClasspath by project.configurations
    dependsOn(compileClasspath)

    doLast {
        val files = compileClasspath.resolvedConfiguration.files
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

        val depList = DepList(ideaVersion, ideaVersionName, deps.sortedWith(compareBy<Dep> { it.groupId }.thenBy { it.artifactId }))
        jsonFile.bufferedWriter().use { writer ->
            GsonBuilder().setPrettyPrinting().create().toJson(depList, writer)
        }
    }
}
