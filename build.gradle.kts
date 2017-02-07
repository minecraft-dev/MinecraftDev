/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

import net.minecrell.gradle.licenser.LicenseExtension
import net.minecrell.gradle.licenser.Licenser
import org.gradle.api.file.CopySpec
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

buildscript {
    repositories {
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
        maven {
            name = "kotlin-eap-1.1"
            setUrl("https://dl.bintray.com/kotlin/kotlin-eap-1.1")
        }
        maven {
            setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service")
        }
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", project.properties["kotlinVersion"] as String))
        classpath("gradle.plugin.org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.2.0")
        classpath("gradle.plugin.net.minecrell:licenser:0.3")
    }
}

val ideaVersion by project
val javaVersion by project
val kotlinVersion by project
val pluginVersion by project
val pluginGroup by project
val downloadIdeaSources by project

apply {
    plugin<IdeaPlugin>()
    plugin<IntelliJPlugin>()
    plugin<KotlinPluginWrapper>()
    plugin<JavaPlugin>()
    plugin<GroovyPlugin>()
    plugin<Licenser>()
}

group = pluginGroup
version = pluginVersion

repositories {
    maven {
        name = "kotlin-eap-1.1"
        setUrl("https://dl.bintray.com/kotlin/kotlin-eap-1.1")
    }
}

dependencies {
    compile(kotlinModule("stdlib-jre8", kotlinVersion as String) as String) {
        // JetBrains annotations are already bundled with IntelliJ IDEA
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // Add tools.jar for the JDI API
    compile(files(org.gradle.internal.jvm.Jvm.current().toolsJar))

    // Add an additional dependency on kotlin-runtime. It is essentially useless
    // (since kotlin-runtime is a transitive dependency of kotlin-stdlib-jre8)
    // but without kotlin-stdlib or kotlin-runtime on the classpath,
    // gradle-intellij-plugin will add IntelliJ IDEA's Kotlin version to the
    // dependencies which conflicts with our newer version.
    compile(kotlinModule("runtime", kotlinVersion as String) as String) {
        isTransitive = false
    }
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

configure<JavaPluginConvention> {
    setSourceCompatibility(javaVersion)
    setTargetCompatibility(javaVersion)
}

configure<IdeaModel> {
    module.apply {
        generatedSourceDirs.add(file("gen"))
        excludeDirs.add(file(the<IntelliJPluginExtension>().sandboxDirectory))
    }
}

// License header formatting
configure<LicenseExtension> {
    header = file("copyright.txt")
    include("**/*.java", "**/*.kt", "**/*.groovy", "**/*.gradle", "**/*.xml", "**/*.properties", "**/*.html")
    exclude(
        "com/demonwav/mcdev/platform/mcp/at/gen/**",
        "**messages.MinecraftDevelopment.properties",
        "**messages.MinecraftDevelopment_en.properties"
    )

    newLine = true
}

(tasks.getByName("processResources") as CopySpec).apply {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.MinecraftDevelopment_en_US.properties") {
            rename { "messages.MinecraftDevelopment$lang.properties" }
        }
    }
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

    inputs.file(src)
    outputs.file(dst + lexerFileName)
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

    val src = "src/main/java/com/demonwav/mcdev/platform/mcp/at/AT.bnf"
    val dstRoot = "gen"
    val dst = "$dstRoot/com/demonwav/mcdev/platform/mcp/at/gen"
    val psiDir = "$dst/psi/"
    val parserDir = "$dst/parser/"

    doFirst {
        delete(psiDir, parserDir)
    }

    main = "org.intellij.grammar.Main"

    args(dstRoot, src)

    inputs.file(src)
    outputs.dirs(mapOf(
            "psi" to psiDir,
            "parser" to parserDir
    ))

    classpath(pathingJar.archivePath, file("libs/grammar-kit-1.5.0.jar"))
}

val generate = task("generate") {
    dependsOn(generateAtLexer, generateAtPsiAndParser)
}

the<JavaPluginConvention>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java.srcDir("gen")

tasks.withType<JavaCompile> {
    dependsOn(generate)
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    dependsOn(generate)
    kotlinOptions.jvmTarget = javaVersion as String
}

if (project.hasProperty("intellijJre")) {
    afterEvaluate {
        (tasks.getByName("runIdea") as JavaExec).apply {
            executable(project.properties["intellijJre"] as String)
        }
    }
}

defaultTasks("build")
