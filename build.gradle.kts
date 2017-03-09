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
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.jvm.Jvm
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.HashMap
import kotlin.reflect.KProperty

buildscript {
    repositories {
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
        maven {
            setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service")
        }
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", properties["kotlinVersion"]))
        classpath("gradle.plugin.org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.2.5")
        classpath("gradle.plugin.net.minecrell:licenser:0.3")
    }
}

// Get rid of pointless casts to String - Start
operator fun getValue(thisRef: Any, property: KProperty<*>) = project.getValue(thisRef, property) as String
class StringMap(val map: Map<String, *>) : HashMap<String, String>() { operator override fun get(key: String) = map[key] as String? }
val properties = StringMap(project.properties)
fun KotlinDependencyHandler.kotlinModule(module: String) = kotlinModule(module, kotlinVersion) as String
// End

val ideaVersion by this
val javaVersion by this
val kotlinVersion by this
val pluginVersion by this
val pluginGroup by this
val downloadIdeaSources by this

apply {
    plugin<JavaPlugin>()
    plugin<KotlinPluginWrapper>()
    plugin<GroovyPlugin>()
    plugin<IdeaPlugin>()
    plugin<IntelliJPlugin>()
    plugin<Licenser>()
}

group = pluginGroup
version = pluginVersion

configurations.create("mixin").isTransitive = false

repositories {
    mavenCentral()
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

    add("mixin", "org.spongepowered:mixin:0.6.8-SNAPSHOT:thin")
}

tasks.withType<Test> {
    doFirst {
        if (System.getenv()["CI"] != null) {
            systemProperty("slowCI", "true")
        }

        systemProperty("mixinUrl", configurations.getByName("mixin").files.first().absolutePath)
    }
}

configure<IntelliJPluginExtension> {
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    setPlugins("maven", "gradle", "Groovy",
               // needed dependencies for unit tests
               "properties", "junit")

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = false

    downloadSources = System.getenv()["CI"] == null && downloadIdeaSources.toBoolean()

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

// Credit for this intellij-rust
// https://github.com/intellij-rust/intellij-rust/blob/d6b82e6aa2f64b877a95afdd86ec7b84394678c3/build.gradle#L131-L181
val generateAtLexer = task<JavaExec>("generateAtLexer") {
    val src = "src/main/grammars/AtLexer.flex"
    val skeleton = "libs/idea-flex.skeleton"
    val dst = "gen/com/demonwav/mcdev/platform/mcp/at/gen/"
    val output = "$dst/AtLexer.java"

    doFirst {
        delete(output)
    }

    classpath = files("libs/jflex-1.7.0-SNAPSHOT.jar")
    main = "jflex.Main"

    args(
        "--skel", skeleton,
        "-d", dst,
        src
    )

    inputs.files(src, skeleton)
    outputs.file(output)
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

    val src = "src/main/grammars/AtParser.bnf".replace("/", File.separator)
    val dstRoot = "gen"
    val dst = "$dstRoot/com/demonwav/mcdev/platform/mcp/at/gen".replace("/", File.separator)
    val psiDir = "$dst/psi/".replace("/", File.separator)
    val parserDir = "$dst/parser/".replace("/", File.separator)

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

    classpath(pathingJar.archivePath, file("libs/grammar-kit-1.5.1.jar"))
}

val generate = task("generate") {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    dependsOn(generateAtLexer, generateAtPsiAndParser)
}

the<JavaPluginConvention>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java.srcDir("gen")

tasks.withType<JavaCompile> {
    dependsOn(generate)
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    dependsOn(generate)
    kotlinOptions.jvmTarget = javaVersion
}

// Remove gen directory on clean
tasks.getByName("clean").doLast {
    delete(file("gen"))
}

project.findProperty("intellijJre")
    ?.let { (tasks.getByName("runIde") as JavaExec).setExecutable(it) }

defaultTasks("build")
