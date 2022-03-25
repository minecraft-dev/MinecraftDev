/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

import org.cadixdev.gradle.licenser.header.HeaderStyle
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    java
    mcdev
    groovy
    idea
    val ijPluginVersion = if (System.getProperty("mcdev.localdev").toBoolean()) {
        "1.1.3"
    } else {
        "1.3.0"
    }
    id("org.jetbrains.intellij") version ijPluginVersion
    id("org.cadixdev.licenser") version "0.6.1"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

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
    maven("https://repo.denwav.dev/repository/maven-public/")
    mavenCentral()
}

dependencies {
    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(libs.bundles.coroutines)

    implementation(files(gradleToolingExtensionJar))

    implementation(libs.templateMakerFabric)
    implementation(libs.bundles.asm)

    jflex(libs.jflex.lib)
    jflexSkeleton(libs.jflex.skeleton) {
        artifact {
            extension = "skeleton"
        }
    }
    grammarKit(libs.grammarKit)

    testLibs(libs.test.mockJdk)
    testLibs(libs.test.mixin)
    testLibs(libs.test.spongeapi) {
        artifact {
            classifier = "shaded"
        }
    }
    testLibs(libs.test.nbt) {
        artifact {
            extension = "nbt"
        }
    }
    testLibs(project(":mixin-test-data"))

    // For non-SNAPSHOT versions (unless Jetbrains fixes this...) find the version with:
    // afterEvaluate { println(intellij.ideaDependency.buildNumber.substring(intellij.type.length + 1)) }
    gradleToolingExtension(libs.gradleToolingExtension)
    gradleToolingExtension(libs.annotations)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.entine)
}

intellij {
    // IntelliJ IDEA dependency
    version.set(ideaVersion)
    // Bundled plugin dependencies
    plugins.addAll(
        "java",
        "maven",
        "gradle",
        "Groovy",
        "org.toml.lang:$pluginTomlVersion",
        "ByteCodeViewer",
        // needed dependencies for unit tests
        "properties",
        "junit"
    )

    pluginName.set("Minecraft Development")
    updateSinceUntilBuild.set(true)

    downloadSources.set(downloadIdeaSources.toBoolean())

    sandboxDir.set(layout.projectDirectory.dir(".sandbox").toString())
}

tasks.publishPlugin {
    // Build numbers are used for
    properties["buildNumber"]?.let { buildNumber ->
        project.version = "${project.version}-$buildNumber"
    }
    properties["mcdev.deploy.token"]?.let { deployToken ->
        token.set(deployToken.toString())
    }
    channels.add(properties["mcdev.deploy.channel"]?.toString() ?: "Stable")
}

tasks.runPluginVerifier {
    ideVersions.addAll("IC-2020.1.3", "IC-2020.1.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-proc:none")
    options.release.set(11)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xjvm-default=enable")
    }
}

// Compile classes to be loaded into the Gradle VM to Java 5 to match Groovy
// This is for maximum compatibility, these classes will be loaded into every Gradle import on all
// projects (not just Minecraft), so we don't want to break that with an incompatible class version.
tasks.named(gradleToolingExtensionSourceSet.compileJavaTaskName, JavaCompile::class) {
    val java7Compiler = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }
    javaCompiler.set(java7Compiler)
    options.release.set(null as Int?)
    sourceCompatibility = "1.5"
    targetCompatibility = "1.5"
    options.bootstrapClasspath = files(java7Compiler.map { it.metadata.installationPath.file("jre/lib/rt.jar") })
    options.compilerArgs = listOf("-Xlint:-options")
}
tasks.withType<GroovyCompile>().configureEach {
    options.compilerArgs = listOf("-proc:none")
    sourceCompatibility = "1.5"
    targetCompatibility = "1.5"
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
            relativePath = RelativePath(true, "fileTemplates", "j2ee", this.name)
        }
    }
}

tasks.test {
    dependsOn(tasks.jar, testLibs)
    useJUnitPlatform()
    doFirst {
        testLibs.resolvedConfiguration.resolvedArtifacts.forEach {
            systemProperty("testLibs.${it.name}", it.file.absolutePath)
        }
    }
    systemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")

    jvmArgs(
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.ref=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED",
        "--add-opens", "java.base/java.util.concurrent.locks=ALL-UNNAMED",
        "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.fs=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt.event=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
        "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.font=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.swing=ALL-UNNAMED",
    )
}

idea {
    module {
        generatedSourceDirs.add(file("build/gen"))
        excludeDirs.add(file(intellij.sandboxDir.get()))
    }
}

license {
    header.set(resources.text.fromFile(file("copyright.txt")))
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
        "com/demonwav/mcdev/platform/mcp/aw/gen/**",
        "com/demonwav/mcdev/nbt/lang/gen/**",
        "com/demonwav/mcdev/platform/mixin/invalidInjectorMethodSignature/*.java",
        "com/demonwav/mcdev/translations/lang/gen/**"
    )

    tasks {
        register("gradle") {
            files.from(
                fileTree(project.projectDir) {
                    include("*.gradle.kts", "gradle.properties")
                    exclude("**/buildSrc/**", "**/build/**")
                }
            )
        }
        register("buildSrc") {
            files.from(
                project.fileTree(project.projectDir.resolve("buildSrc")) {
                    include("**/*.kt", "**/*.kts")
                    exclude("**/build/**")
                }
            )
        }
        register("grammars") {
            files.from(project.fileTree("src/main/grammars"))
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

val generateAwLexer by lexer("AwLexer", "com/demonwav/mcdev/platform/mcp/aw/gen")
val generateAwParser by parser("AwParser", "com/demonwav/mcdev/platform/mcp/aw/gen")

val generateNbttLexer by lexer("NbttLexer", "com/demonwav/mcdev/nbt/lang/gen")
val generateNbttParser by parser("NbttParser", "com/demonwav/mcdev/nbt/lang/gen")

val generateLangLexer by lexer("LangLexer", "com/demonwav/mcdev/translations/lang/gen")
val generateLangParser by parser("LangParser", "com/demonwav/mcdev/translations/lang/gen")

val generateTranslationTemplateLexer by lexer("TranslationTemplateLexer", "com/demonwav/mcdev/translations/lang/gen")

val generate by tasks.registering {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    outputs.dir(layout.buildDirectory.dir("gen"))
    dependsOn(
        generateAtLexer,
        generateAtParser,
        generateAwLexer,
        generateAwParser,
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

tasks.register("cleanSandbox", Delete::class) {
    group = "intellij"
    description = "Deletes the sandbox directory."
    delete(layout.projectDirectory.dir(".sandbox"))
}

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

tasks.buildSearchableOptions {
    // not working atm
    enabled = false
    // https://youtrack.jetbrains.com/issue/IDEA-210683
    jvmArgs(
        "--illegal-access=deny",
        "--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.font=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.swing=ALL-UNNAMED"
    )

    if (OperatingSystem.current().isMacOsX) {
        jvmArgs("--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
    }
}
