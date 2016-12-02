/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

import net.minecrell.gradle.licenser.LicenseExtension
import net.minecrell.gradle.licenser.Licenser
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenCentral()
        gradleScriptKotlin()
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
        maven {
            setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service")
        }
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
        classpath("gradle.plugin.org.jetbrains:gradle-intellij-plugin:0.1.10")
        classpath("gradle.plugin.net.minecrell:licenser:0.3")
    }
}

val ideaVersion by project
val javaVersion by project
val pluginVersion by project
val pluginGroup by project
val downloadIdeaSources by project

apply {
    plugin<IdeaPlugin>()
    plugin<IntelliJPlugin>()
    plugin<KotlinPluginWrapper>()
    plugin<JavaPlugin>()
    plugin<Licenser>()
}


group = pluginGroup
version = pluginVersion

repositories {
    gradleScriptKotlin()
}

dependencies {
    compile(kotlinModule("stdlib-jre8"))
}

configure<IntelliJPluginExtension> {
    // IntelliJ IDEA dependency
    version =  if (project.hasProperty("intellijVersion")) {
        project.properties["intellijVersion"] as String
    } else {
        ideaVersion as String
    }
    // Bundled plugin dependencies
    setPlugins("maven", "gradle", "Groovy", "yaml",
        // needed dependencies for unit tests
        "properties", "junit")

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = false

    downloadSources = (downloadIdeaSources as String).toBoolean()
    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

configure<IdeaModel> {
    project.apply {
        jdkName = "1.8"
        setLanguageLevel("1.8")
    }

    module.apply {
        excludeDirs.add(file(the<IntelliJPluginExtension>().sandboxDirectory))
    }
}

// License header formatting
configure<LicenseExtension> {
    header = file("copyright.txt")
    include("**/*.java", "**/*.kt", "**/*.gradle", "**/*.xml", "**/*.properties", "**/*.html")
    exclude(
        "com/demonwav/mcdev/platform/mcp/at/gen/**",
        "**messages.MinecraftDevelopment.properties",
        "**messages.MinecraftDevelopment_en.properties"
    )

    newLine = true
}

val initPropTask = task("initProp") {
    val baseProp = File("src/main/resources/messages.MinecraftDevelopment_en.properties")
    val baseEnglishProp = File("src/main/resources/messages.MinecraftDevelopment.properties")

    val comment =
        "# Do not manually edit this file\n" +
        "# This file is automatically copied from messages.MinecraftDevelopment_en_US.properties at build time\n"

    val baseUsEnglish = File("src/main/resources/messages.MinecraftDevelopment_en_US.properties")

    baseProp.writeText(comment + baseUsEnglish.readText())
    baseEnglishProp.writeText(comment + baseUsEnglish.readText())
}

// Credit for this intellij-rust https://github.com/intellij-rust/intellij-rust/blob/master/build.gradle#L114
val generateAtLexer = task<JavaExec>("generateAtLexer") {
    val src = "src/main/java/com/demonwav/mcdev/platform/mcp/at/AT.flex"
    val dst = "gen/com/demonwav/mcdev/platform/mcp/at/gen/"
    val lexerFileName = "AtLexer.java"

    classpath = files("libs/jflex-1.7.0-SNAPSHOT.jar")
    main = "jflex.Main"

    args(
        "--skel", "libs/idea-flex.skeleton",
        "-d", dst,
        src
    )

    inputs.file(file(src))
    inputs.dir(file(dst + lexerFileName))
}

/*
 * This helps us get around the command length issues on Windows by placing the classpath in the manifest of a single
 * jar, rather than printing them out in one long line
 */
val pathingJar = task<Jar>("pathingJar") {
    dependsOn(configurations.compile)
    appendix = "pathing"

    doFirst {
        manifest.apply {
            attributes["Class-Path"] = configurations.compile.files.map { file ->
                file.toURI().toString().replaceFirst("file:/+".toRegex(), "/")
            }.joinToString(" ")
        }
    }
}

val generateAtPsiAndParser = task<JavaExec>("generateAtPsiAndParser") {
    dependsOn(pathingJar)
    doFirst {
        delete(file("gen/com/demonwav/mcdev/platform/mcp/at/gen/psi/"))
    }

    val src = "src/main/java/com/demonwav/mcdev/platform/mcp/at/AT.bnf"
    val dstRoot = "gen"

    main = "org.intellij.grammar.Main"

    args(dstRoot, src)

    inputs.file(file(src))
    outputs.dir(fileTree(mapOf(
        "dir" to dstRoot + "/com/demonwav/mcdev/platform/mcp/at/gen/",
        "include" to "**/*.java"
    )))

    classpath("$buildDir/classes/main", "$buildDir/resources/main", pathingJar.archivePath, fileTree(mapOf(
        "dir" to "libs/",
        "include" to "**/*.jar"
    )))
}

val generate = task("generate") {
    dependsOn(generateAtLexer, generateAtPsiAndParser)
}

val java = the<JavaPluginConvention>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java
java.setSrcDirs(arrayListOf(java.srcDirs, file("gen")))

tasks.withType<JavaCompile> {
    dependsOn(generate)
    options.encoding = "UTF-8"
    sourceCompatibility = javaVersion as String
    targetCompatibility = javaVersion as String
}

tasks.withType<KotlinCompile> {
    dependsOn(generate)
    kotlinOptions.jvmTarget = javaVersion as String
}

afterEvaluate {
    getTasksByName("prepareSandbox", false).forEach { it.dependsOn("initProp") }
}

defaultTasks("build")
