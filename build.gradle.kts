/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

import net.minecrell.gradle.licenser.header.HeaderStyle
import org.gradle.internal.jvm.Jvm
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
}

plugins {
    kotlin("jvm") version "1.3.31" // kept in sync with IntelliJ's bundled dep
    groovy
    idea
    id("org.jetbrains.intellij") version "0.4.15"
    id("net.minecrell.licenser") version "0.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1"
}

group = "com.demonwav.minecraft-dev"

val coroutineVersion = "1.2.1" // Coroutine version also kept in sync with IntelliJ's bundled dep

defaultTasks("build")

val ideaVersion: String by project
val downloadIdeaSources: String by project

// for publishing nightlies
val repoToken: String by project
val repoChannel: String by project

val compileKotlin by tasks.existing
val processResources by tasks.existing<AbstractCopyTask>()
val test by tasks.existing<Test>()
val runIde by tasks.existing<RunIdeTask>()
val buildSearchableOptions by tasks.existing<BuildSearchableOptionsTask>()
val publishPlugin by tasks.existing<PublishTask>()
val clean by tasks.existing<Delete>()

// configurations
val idea by configurations
val gradleToolingExtension: Configuration by configurations.creating {
    extendsFrom(idea)
}
val jflex: Configuration by configurations.creating
val jflexSkeleton: Configuration by configurations.creating
val grammarKit: Configuration by configurations.creating
val testLibs: Configuration by configurations.creating {
    isTransitive = false
}

val gradleToolingExtensionSourceSet = sourceSets.create("gradle-tooling-extension") {
    configurations.named(compileOnlyConfigurationName) {
        extendsFrom(gradleToolingExtension)
    }
}
val gradleToolingExtensionJar = tasks.register<Jar>(gradleToolingExtensionSourceSet.jarTaskName) {
    from(gradleToolingExtensionSourceSet.output)
    archiveClassifier.set("gradle-tooling-extension")
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/minecraft-dev/maven")
    maven("https://repo.spongepowered.org/maven")
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
}

// Sources aren't provided through the gradle intellij plugin for bundled libs, use compileOnly to attach them
// but not include them in the output artifact
//
// Kept in a separate block for readability
dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    compileOnly("org.apache.commons:commons-lang3:3.9")
}

dependencies {
    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))

    implementation(files(gradleToolingExtensionJar))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutineVersion") {
        isTransitive = false
    }

    jflex("org.jetbrains.idea:jflex:1.7.0-b7f882a")
    jflexSkeleton("org.jetbrains.idea:jflex:1.7.0-c1fdf11:idea@skeleton")
    grammarKit("org.jetbrains.idea:grammar-kit:1.5.1")

    testLibs("org.jetbrains.idea:mockJDK:1.7-4d76c50")
    testLibs("org.spongepowered:mixin:0.7-SNAPSHOT:thin")
    testLibs("com.demonwav.mcdev:all-types-nbt:1.0@nbt")

    // For non-SNAPSHOT versions (unless Jetbrains fixes this...) find the version with:
    // println(intellij.ideaDependency.buildNumber.substring(intellij.type.length + 1))
    gradleToolingExtension("com.jetbrains.intellij.gradle:gradle-tooling-extension:193.5233.102")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}

intellij {
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    setPlugins(
        "java", "maven", "gradle", "Groovy",
        // needed dependencies for unit tests
        "properties", "junit",
        // useful to have when running for mods.toml
        "org.toml.lang:0.2.111.34-193"
    )

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = true

    downloadSources = downloadIdeaSources.toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

publishPlugin {
    if (properties["publish"] != null) {
        project.version = "${project.version}-${properties["buildNumber"]}"

        token(repoToken)
        channels(repoChannel)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-proc:none")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

tasks.withType<GroovyCompile>().configureEach {
    options.compilerArgs = listOf("-proc:none")
}

processResources {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.MinecraftDevelopment_en_US.properties") {
            rename { "messages.MinecraftDevelopment$lang.properties" }
        }
    }
}

test {
    dependsOn(testLibs)
    useJUnitPlatform()
    doFirst {
        testLibs.resolvedConfiguration.resolvedArtifacts.forEach {
            systemProperty("testLibs.${it.name}", it.file.absolutePath)
        }
    }
    if (JavaVersion.current().isJava9Compatible) {
        jvmArgs(
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
            "--add-opens", "java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.font=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.swing=ALL-UNNAMED"
        )
    }
}

idea {
    module {
        generatedSourceDirs.add(file("gen"))
        excludeDirs.add(file(intellij.sandboxDirectory))
    }
}

license {
    header = file("copyright.txt")
    style["flex"] = HeaderStyle.BLOCK_COMMENT.format
    style["bnf"] = HeaderStyle.BLOCK_COMMENT.format

    include(
        "**/*.java",
        "**/*.kt",
        "**/*.kts",
        "**/*.groovy",
        "**/*.gradle",
        "**/*.xml",
        "**/*.properties",
        "**/*.html",
        "**/*.flex",
        "**/*.bnf"
    )
    exclude(
        "com/demonwav/mcdev/platform/mcp/at/gen/**",
        "com/demonwav/mcdev/nbt/lang/gen/**",
        "com/demonwav/mcdev/i18n/lang/gen/**"
    )

    tasks {
        register("gradle") {
            files = project.files("build.gradle.kts", "settings.gradle.kts", "gradle.properties")
        }
        register("grammars") {
            files = project.fileTree("src/main/grammars")
        }
    }
}

tasks.register("format") {
    group = "minecraft"
    description = "Formats source code according to project style"
    val licenseFormat by tasks.existing
    val ktlintFormat by tasks.existing
    dependsOn(licenseFormat, ktlintFormat)
}

// Credit for this intellij-rust
// https://github.com/intellij-rust/intellij-rust/blob/d6b82e6aa2f64b877a95afdd86ec7b84394678c3/build.gradle#L131-L181
fun generateLexer(name: String, flex: String, pack: String) = tasks.register<JavaExec>(name) {
    val src = "src/main/grammars/$flex.flex"
    val dst = "gen/com/demonwav/mcdev/$pack"
    val output = "$dst/$flex.java"

    classpath = jflex
    main = "jflex.Main"

    doFirst {
        args(
            "--skel", jflexSkeleton.singleFile.absolutePath,
            "-d", dst,
            src
        )

        // Delete current lexer
        delete(output)
    }

    inputs.files(src, jflexSkeleton)
    outputs.file(output)
}

fun generatePsiAndParser(name: String, bnf: String, pack: String) = tasks.register<JavaExec>(name) {
    val src = "src/main/grammars/$bnf.bnf".replace('/', File.separatorChar)
    val dstRoot = "gen"
    val dst = "$dstRoot/com/demonwav/mcdev/$pack".replace('/', File.separatorChar)
    val psiDir = "$dst/psi/".replace('/', File.separatorChar)
    val parserDir = "$dst/parser/".replace('/', File.separatorChar)

    doFirst {
        delete(psiDir, parserDir)
    }

    classpath = grammarKit
    main = "org.intellij.grammar.Main"

    if (JavaVersion.current().isJava9Compatible) {
        jvmArgs(
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED"
        )
    }

    args(dstRoot, src)

    inputs.file(src)
    outputs.dirs(
        mapOf(
            "psi" to psiDir,
            "parser" to parserDir
        )
    )
}

val generateAtLexer = generateLexer("generateAtLexer", "AtLexer", "platform/mcp/at/gen/")
val generateAtPsiAndParser = generatePsiAndParser("generateAtPsiAndParser", "AtParser", "platform/mcp/at/gen")

val generateNbttLexer = generateLexer("generateNbttLexer", "NbttLexer", "nbt/lang/gen/")
val generateNbttPsiAndParser = generatePsiAndParser("generateNbttPsiAndParser", "NbttParser", "nbt/lang/gen")

val generateI18nLexer = generateLexer("generateI18nLexer", "I18nLexer", "i18n/lang/gen/")
val generateI18nPsiAndParser = generatePsiAndParser("generateI18nPsiAndParser", "I18nParser", "i18n/lang/gen")

val generateI18nTemplateLexer = generateLexer("generateI18nTemplateLexer", "I18nTemplateLexer", "i18n/lang/gen/")

val generate by tasks.registering {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    dependsOn(
        generateAtLexer,
        generateAtPsiAndParser,
        generateNbttLexer,
        generateNbttPsiAndParser,
        generateI18nLexer,
        generateI18nPsiAndParser,
        generateI18nTemplateLexer
    )
    outputs.dir("gen")
}

sourceSets.named("main") { java.srcDir(generate) }

// Remove gen directory on clean
clean { delete(generate) }

runIde {
    maxHeapSize = "2G"

    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }
}

inline fun <reified T : Task> TaskContainer.existing() = existing(T::class)
inline fun <reified T : Task> TaskContainer.register(name: String, configuration: Action<in T>) =
    register(name, T::class, configuration)
