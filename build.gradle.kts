/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

import org.cadixdev.gradle.licenser.header.HeaderStyle
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
}

plugins {
    kotlin("jvm") version "1.4.21" // kept in sync with IntelliJ's bundled dep
    java
    mcdev
    groovy
    idea
    id("org.jetbrains.intellij") version "0.6.5"
    id("org.cadixdev.licenser") version "0.5.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}

val coroutineVersion = "1.4.2" // Coroutine version also kept in sync with IntelliJ's bundled dep

val ideaVersion: String by project
val ideaVersionName: String by project
val coreVersion: String by project
val downloadIdeaSources: String by project
val pluginTomlVersion: String by project

// configurations
val idea by configurations
val jflex by configurations
val jflexSkeleton by configurations
val grammarKit by configurations

val gradleToolingExtension: Configuration by configurations.creating {
    extendsFrom(idea)
}
val testLibs: Configuration by configurations.creating {
    isTransitive = false
}

group = "com.demonwav.minecraft-dev"
version = "$ideaVersionName-$coreVersion"

val gradleToolingExtensionSourceSet: SourceSet = sourceSets.create("gradle-tooling-extension") {
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
    maven("https://repo.gradle.org/gradle/libs-releases-local/")
    maven("https://maven.extracraftx.com")
}

dependencies {
    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))

    // Kotlin
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutineVersion") {
        isTransitive = false
    }

    implementation(files(gradleToolingExtensionJar))

    implementation("com.extracraftx.minecraft:TemplateMakerFabric:0.3.0")

    jflex("org.jetbrains.idea:jflex:1.7.0-b7f882a")
    jflexSkeleton("org.jetbrains.idea:jflex:1.7.0-c1fdf11:idea@skeleton")
    grammarKit("org.jetbrains.idea:grammar-kit:1.5.1")

    testLibs("org.jetbrains.idea:mockJDK:1.7-4d76c50")
    testLibs("org.spongepowered:mixin:0.7-SNAPSHOT:thin")
    testLibs("com.demonwav.mcdev:all-types-nbt:1.0@nbt")
    testLibs("org.spongepowered:spongeapi:7.0.0:shaded")

    // For non-SNAPSHOT versions (unless Jetbrains fixes this...) find the version with:
    // afterEvaluate { println(intellij.ideaDependency.buildNumber.substring(intellij.type.length + 1)) }
    gradleToolingExtension("com.jetbrains.intellij.gradle:gradle-tooling-extension:211-EAP-SNAPSHOT")
    gradleToolingExtension("org.jetbrains:annotations:20.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}

intellij {
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    setPlugins(
        "java",
        "maven",
        "gradle",
        "Groovy",
        // needed dependencies for unit tests
        "properties",
        "junit",
        "org.toml.lang:$pluginTomlVersion"
    )

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = true

    downloadSources = downloadIdeaSources.toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

tasks.publishPlugin {
    // Build numbers are used for
    properties["buildNumber"]?.let { buildNumber ->
        project.version = "${project.version}-$buildNumber"
    }
    properties["mcdev.deploy.token"]?.let { deployToken ->
        token(deployToken)
    }
    channels(properties["mcdev.deploy.channel"] ?: "Stable")
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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xjvm-default=enable")
    }
    this.javaPackagePrefix
}

tasks.withType<GroovyCompile>().configureEach {
    options.compilerArgs = listOf("-proc:none")
}

tasks.processResources {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.MinecraftDevelopment_en_US.properties") {
            rename { "messages.MinecraftDevelopment$lang.properties" }
        }
    }
    // These templates aren't allowed to be in a directory structure in the output jar
    // But we have a lot of templates that would get real hard to deal with if we didn't have some structure
    // So this just flattens out the fileTemplates/j2ee directory in the jar, while still letting us have directories
    exclude("fileTemplates/j2ee/**")
    from(fileTree("src/main/resources/fileTemplates/j2ee").files) {
        eachFile {
            this.relativePath = RelativePath(true, "fileTemplates", "j2ee", this.name)
        }
    }
}

tasks.test {
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
        "com/demonwav/mcdev/platform/mixin/invalidInjectorMethodSignature/*.java",
        "com/demonwav/mcdev/translations/lang/gen/**"
    )

    tasks {
        register("gradle") {
            files = project.fileTree(project.projectDir) {
                include("**/*.gradle.kts", "gradle.properties")
                exclude("**/buildSrc/**", "**/build/**")
            }
        }
        register("buildSrc") {
            val buildSrc = project.projectDir.resolve("buildSrc")
            files = project.fileTree(buildSrc) {
                include("**/*.kt", "**/*.kts")
                exclude("**/build/**")
            }
        }
        register("grammars") {
            files = project.fileTree("src/main/grammars")
        }
    }
}

ktlint {
    enableExperimentalRules.set(true)
}

tasks.register("format") {
    group = "minecraft"
    description = "Formats source code according to project style"
    val licenseFormat by tasks.existing
    val ktlintFormat by tasks.existing
    dependsOn(licenseFormat, ktlintFormat)
}

val generateAtLexer by lexer("AtLexer", "com/demonwav/mcdev/platform/mcp/at/gen")
val generateAtParser by parser("AtParser", "com/demonwav/mcdev/platform/mcp/at/gen")

val generateNbttLexer by lexer("NbttLexer", "com/demonwav/mcdev/nbt/lang/gen")
val generateNbttParser by parser("NbttParser", "com/demonwav/mcdev/nbt/lang/gen")

val generateLangLexer by lexer("LangLexer", "com/demonwav/mcdev/translations/lang/gen")
val generateLangParser by parser("LangParser", "com/demonwav/mcdev/translations/lang/gen")

val generateTranslationTemplateLexer by lexer("TranslationTemplateLexer", "com/demonwav/mcdev/translations/lang/gen")

val generate by tasks.registering {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    outputs.dir("gen")
    dependsOn(
        generateAtLexer,
        generateAtParser,
        generateNbttLexer,
        generateNbttParser,
        generateLangLexer,
        generateLangParser,
        generateTranslationTemplateLexer
    )
}

sourceSets.main { java.srcDir(generate) }

// Remove gen directory on clean
tasks.clean { delete(generate) }

tasks.runIde {
    maxHeapSize = "2G"

    jvmArgs("--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED")
    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }
    // Set these properties to test different languages
    // systemProperty("user.language", "en")
    // systemProperty("user.country", "US")
}
