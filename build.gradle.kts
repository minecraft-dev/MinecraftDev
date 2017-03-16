/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.gradle.internal.jvm.Jvm
import org.jetbrains.intellij.tasks.RunIdeaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        maven {
            name = "intellij-plugin-service"
            setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service")
        }
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.1.1"
    groovy
    idea
    id("org.jetbrains.intellij") version "0.2.5"
    id("net.minecrell.licenser") version "0.3"
}

defaultTasks("build")

val CI = System.getenv("CI") != null

val ideaVersion: String by extra
val javaVersion: String by extra
val kotlinVersion: String by extra
val downloadIdeaSources: String by extra

val clean: Delete by tasks
val processResources: AbstractCopyTask by tasks
val runIde: JavaExec by tasks
val compileKotlin by tasks

configurations {
    "jflex"()
    "jflex-skeleton"()
    "grammar-kit"()
    "testLibs" {
        isTransitive = false
    }
}

repositories {
    mavenCentral()
    maven {
        name = "minecraft-dev"
        setUrl("https://dl.bintray.com/minecraft-dev/maven")
    }
    maven {
        name = "sponge"
        setUrl("https://repo.spongepowered.org/maven")
    }
}

dependencies {
    compile(kotlinModule("stdlib-jre8")) {
        // JetBrains annotations are already bundled with IntelliJ IDEA
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // Add tools.jar for the JDI API
    compile(files(Jvm.current().toolsJar))

    // Add an additional dependency on kotlin-runtime. It is essentially useless
    // (since kotlin-runtime is a transitive dependency of kotlin-stdlib-jre8)
    // but without kotlin-stdlib or kotlin-runtime on the classpath,
    // gradle-intellij-plugin will add IntelliJ IDEA's Kotlin version to the
    // dependencies which conflicts with our newer version.
    compile(kotlinModule("runtime")) {
        isTransitive = false
    }

    "jflex"("org.jetbrains.idea:jflex:1.7.0-b7f882a")
    "jflex-skeleton"("org.jetbrains.idea:jflex:1.7.0-c1fdf11:idea@skeleton")
    "grammar-kit"("org.jetbrains.idea:grammar-kit:1.5.1")

    "testLibs"("org.jetbrains.idea:mockJDK:1.7-4d76c50")
    "testLibs"("org.spongepowered:mixin:0.6.8-SNAPSHOT:thin")
}

intellij {
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    setPlugins("maven", "gradle", "Groovy",
        // needed dependencies for unit tests
        "properties", "junit")

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = false

    downloadSources = !CI && downloadIdeaSources.toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

java {
    setSourceCompatibility(javaVersion)
    setTargetCompatibility(javaVersion)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion
}

processResources {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.MinecraftDevelopment_en_US.properties") {
            rename { "messages.MinecraftDevelopment$lang.properties" }
        }
    }
}

tasks.withType<Test> {
    if (CI) {
        systemProperty("slowCI", "true")
    }

    dependsOn(configurations["testLibs"])
    doFirst {
        configurations["testLibs"].resolvedConfiguration.resolvedArtifacts.forEach {
            systemProperty("testLibs.${it.name}", it.file.absolutePath)
        }
    }
}

idea {
    module {
        generatedSourceDirs.add(file("gen"))
        excludeDirs.add(file(intellij().sandboxDirectory))
    }
}

// License header formatting
license {
    header = file("copyright.txt")
    include("**/*.java", "**/*.kt", "**/*.groovy", "**/*.gradle", "**/*.xml", "**/*.properties", "**/*.html")
    exclude("com/demonwav/mcdev/platform/mcp/at/gen/**")
}

// Credit for this intellij-rust
// https://github.com/intellij-rust/intellij-rust/blob/d6b82e6aa2f64b877a95afdd86ec7b84394678c3/build.gradle#L131-L181
val generateAtLexer = task<JavaExec>("generateAtLexer") {
    val src = "src/main/grammars/AtLexer.flex"
    val dst = "gen/com/demonwav/mcdev/platform/mcp/at/gen/"
    val output = "$dst/AtLexer.java"

    classpath = configurations["jflex"]
    main = "jflex.Main"

    doFirst {
        args(
            "--skel", configurations["jflex-skeleton"].singleFile.absolutePath,
            "-d", dst,
            src
        )

        // Delete current lexer
        delete(output)
    }

    inputs.files(src, configurations["jflex-skeleton"])
    outputs.file(output)
}

val generateAtPsiAndParser = task<JavaExec>("generateAtPsiAndParser") {
    val src = "src/main/grammars/AtParser.bnf".replace("/", File.separator)
    val dstRoot = "gen"
    val dst = "$dstRoot/com/demonwav/mcdev/platform/mcp/at/gen".replace("/", File.separator)
    val psiDir = "$dst/psi/".replace("/", File.separator)
    val parserDir = "$dst/parser/".replace("/", File.separator)

    doFirst {
        delete(psiDir, parserDir)
    }

    classpath = configurations["grammar-kit"]
    main = "org.intellij.grammar.Main"

    args(dstRoot, src)

    inputs.file(src)
    outputs.dirs(mapOf(
        "psi" to psiDir,
        "parser" to parserDir
    ))
}

val generate = task("generate") {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    dependsOn(generateAtLexer, generateAtPsiAndParser)
    outputs.dir("gen")
}

tasks.withType<RunIdeaTask> {
    if (System.getProperty("debug") != null) {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.is.internal", "true")
        systemProperty("idea.debug.mode", "true")
    }
}

java().sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.srcDir(generate)

// Workaround for KT-16764
compileKotlin.inputs.dir(generate)

// Use custom JRE for running IntelliJ IDEA when configured
findProperty("intellijJre")?.let(runIde::setExecutable)

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
fun KotlinDependencyHandler.kotlinModule(module: String) = kotlinModule(module, kotlinVersion) as String
