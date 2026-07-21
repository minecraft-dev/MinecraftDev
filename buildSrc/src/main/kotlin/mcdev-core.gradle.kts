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

import org.cadixdev.gradle.licenser.header.HeaderStyle
import org.cadixdev.gradle.licenser.tasks.LicenseTask
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon

plugins {
    java
    idea
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("net.neoforged.licenser")
}

val ideaVersionName: String by project
val coreVersion: String by project
val buildNumber: String? by project

version = "$ideaVersionName-$coreVersion"

// Build numbers are used for nightlies
if (buildNumber != null) {
    version = "$version-nightly+$buildNumber"
}
if (System.getenv("CI") != "true") {
    version = "$version-local"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-proc:none")
    options.release.set(21)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        languageVersion = KotlinVersion.KOTLIN_2_2
        apiVersion = KotlinVersion.KOTLIN_2_2
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs = listOf("-Xjdk-release=21")
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }
}
tasks.withType<CompileUsingKotlinDaemon>().configureEach {
    kotlinDaemonJvmArguments.add("-Xmx2G")
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }

    maven("https://maven.fabricmc.net/") {
        content {
            includeModule("net.fabricmc", "mapping-io")
            includeModule("net.fabricmc", "fabric-loader")
        }
    }
    maven("https://repo.spongepowered.org/maven/") {
        content {
            includeGroup("org.spongepowered")
        }
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        content {
            includeGroup("org.spigotmc")
        }
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        content {
            includeGroup("net.md-5")
        }
    }

    mavenCentral()
    maven("https://repo.denwav.dev/repository/maven-public/")
}

val libs = the<LibrariesForLibs>()
dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.annotations)
    implementation(libs.bundles.coroutines) {
        exclude(module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.vintage) // Hack to get tests to compile and run
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

intellijPlatform {
    sandboxContainer = layout.projectDirectory.dir(".sandbox")

    instrumentCode = false
    buildSearchableOptions = false
}

license {
    header.set(resources.text.fromString($$"""
        Minecraft Development for IntelliJ

        https://mcdev.io/

        Copyright (C) ${year} minecraft-dev

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU Lesser General Public License as published
        by the Free Software Foundation, version 3.0 only.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU Lesser General Public License
        along with this program.  If not, see <https://www.gnu.org/licenses/>.
    """.trimIndent()))

    properties {
        set("year", "2026")
    }

    style["flex"] = HeaderStyle.BLOCK_COMMENT.format
    style["bnf"] = HeaderStyle.BLOCK_COMMENT.format

    val endings = listOf("java", "kt", "kts", "gradle.kts", "xml", "properties", "html", "flex", "bnf")
    include(endings.map { "**/*.$it" })
}

tasks.withType(LicenseTask::class).configureEach {
    val changedFiles = if (rootProject.ext.has("changedFiles")) {
        @Suppress("UNCHECKED_CAST")
        rootProject.ext["changedFiles"] as Set<File>
    } else {
        val unstagedFiles = git("diff", "--name-only").lines()
        val stagedFiles = git("diff", "--staged", "--name-only").lines()
        var changedFiles = (unstagedFiles + stagedFiles)
            .filter { it.isNotBlank() }
            .map(project::file)
            .toSet()
        rootProject.ext["changedFiles"] = changedFiles
        changedFiles
    }
    doFirst {
        files = files.filter {
            it in changedFiles
        }
    }
}

idea {
    module {
        excludeDirs.add(file(intellijPlatform.sandboxContainer.get()))
    }
}

tasks.runIde {
    maxHeapSize = "2G"
    jvmArgs("--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("format") {
    group = "minecraft"
    description = "Formats source code according to project style"
    dependsOn(tasks.licenseFormat)
}
