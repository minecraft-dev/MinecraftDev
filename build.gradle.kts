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

import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.changelog.Changelog
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id(libs.plugins.changelog.get().pluginId)
    alias(libs.plugins.idea.ext)
    `mcdev-core`
    `mcdev-parsing`
    `mcdev-publishing`
    alias(libs.plugins.sentry) apply (System.getenv("CI") == "true" && System.getenv("NO_SENTRY") != "true")
}

val coreVersion = project.property("coreVersion") as String

val gradleToolingExtension = configurations.create("gradleToolingExtension")
val testLibs = configurations.create("testLibs") {
    isTransitive = false
}

group = "com.demonwav.mcdev"

val gradleToolingExtensionSourceSet: SourceSet = sourceSets.create("gradle-tooling-extension", Action<SourceSet> {
    configurations.named(compileOnlyConfigurationName) {
        extendsFrom(gradleToolingExtension)
    }
})
val gradleToolingExtensionJar = tasks.register<Jar>(gradleToolingExtensionSourceSet.jarTaskName) {
    description = "Package Gradle tooling extension jar"
    from(gradleToolingExtensionSourceSet.output)
    archiveClassifier.set("gradle-tooling-extension")
    exclude("META-INF/plugin.xml")
}

val templatesSourceSet: SourceSet = sourceSets.create("templates", Action<SourceSet> {
    resources {
        srcDir("templates")
        compileClasspath += sourceSets.main.get().output
    }
})

val templateSourceSets: List<SourceSet> = (file("templates").listFiles() ?: emptyArray()).mapNotNull { file ->
    if (file.isDirectory() && (file.listFiles() ?: emptyArray()).any { it.name.endsWith(".mcdev.template.json") }) {
        sourceSets.create("templates-${file.name}", Action<SourceSet> {
            resources {
                srcDir(file)
                compileClasspath += sourceSets.main.get().output
            }
        })
    } else {
        null
    }
}

// Code shared by the test source sets: the HTTP mock server, its fixture loader, and the fixture recorder
val testSupportSourceSet: SourceSet = sourceSets.create("testSupport")
val httpFixtureRecorder = configurations.create("httpFixtureRecorder")

val httpFixturesDir: Directory = layout.projectDirectory.dir("src/test/resources/http-fixtures")
val generatedHttpFixturesDir: Provider<Directory> = layout.buildDirectory.dir("http-fixtures-generated")

// The builtin template provider downloads https://github.com/minecraft-dev/templates/archive/refs/heads/v<N>.zip.
// Rather than recording it, zip the pinned templates checkout the same way GitHub does, so the mock server always
// serves the templates the tests run against.
val builtinTemplatesFormatVersion = Regex("""branch\s*=\s*v(\d+)""")
    .find(file(".gitmodules").readText())?.groupValues?.get(1)
    ?: throw GradleException("Couldn't find the templates branch in .gitmodules")
val builtinTemplatesHttpFixture = tasks.register<Zip>("builtinTemplatesHttpFixture") {
    description = "Zip the templates checkout as the HTTP mock server's copy of the builtin template download"
    val version = builtinTemplatesFormatVersion
    val path = "github.com/minecraft-dev/templates/archive/refs/heads/v$version.zip"
    val root = generatedHttpFixturesDir
    val mapping = root.map { it.file("mappings/$path.json") }

    from("templates") {
        exclude(".git")
        into("templates-$version")
    }
    val destination = root.map { it.dir("__files/${path.substringBeforeLast('/')}") }
    destinationDirectory.set(destination)
    archiveFileName.set(path.substringAfterLast('/'))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    // The same WireMock mapping the recorder would write
    outputs.file(mapping)
    // Clear out the files for an older templates format version
    doFirst {
        root.get().asFile.deleteRecursively()
        destination.get().asFile.mkdirs()
    }
    doLast {
        mapping.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                |{
                |  "name": "https://$path",
                |  "request": {
                |    "method": "GET",
                |    "url": "/$path"
                |  },
                |  "response": {
                |    "status": 200,
                |    "bodyFileName": "$path",
                |    "headers": {
                |      "Content-Type": "application/zip"
                |    }
                |  }
                |}
                |""".trimMargin()
            )
        }
    }
}

tasks.register<JavaExec>("recordHttpFixtures") {
    group = "minecraft"
    description = "Record the creator's HTTP fixtures from the live services. " +
        "Pass -Pmcdev.recordFilter=<a>,<b> to only record URLs containing one of those strings."
    // The recorder uses a few platform-free classes from the plugin, such as NpmRegistry
    classpath(testSupportSourceSet.output, sourceSets.main.get().output, httpFixtureRecorder)
    mainClass.set("com.demonwav.mcdev.framework.http.recorder.HttpFixtureRecorder")
    val filters = providers.gradleProperty("mcdev.recordFilter")
    val templatesDir = file("templates")
    val stagingDir = temporaryDir.resolve("http-fixtures")
    argumentProviders += CommandLineArgumentProvider {
        listOf(
            httpFixturesDir.asFile.absolutePath,
            "--templates",
            templatesDir.absolutePath,
            "--staging",
            stagingDir.absolutePath,
        ) +
            filters.orNull?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    }
}

val externalAnnotationsJar = tasks.register<Jar>("externalAnnotationsJar") {
    description = "Package external annotations jar"
    from("externalAnnotations")
    destinationDirectory.set(layout.buildDirectory.dir("externalAnnotations"))
    archiveFileName.set("externalAnnotations.jar")
}

dependencies {
    implementation(files(gradleToolingExtensionJar))

    implementation(libs.mixinExtras.expressions) {
        exclude(group = "org.ow2.asm", module = "asm-debug-all")
    }
    testLibs(libs.mixinExtras.common)
    implementation(libs.jgraphx)

    implementation(libs.mappingIo)
    implementation(libs.bundles.asm)

    implementation(libs.sentry) {
        exclude(group = "org.slf4j")
    }

    implementation(libs.jspecify)

    intellijPlatform {
        intellijIdea(libs.versions.intellij.ide) {
            useInstaller = false
        }

        // Bundled plugin dependencies
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.idea.reposearch")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("ByteCodeViewer")
        bundledModule("intellij.platform.langInjection")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("Git4Idea")
        bundledModule("intellij.platform.collaborationTools")
        bundledPlugin("com.intellij.modules.json")

        // Optional dependencies
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("org.toml.lang")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("org.jetbrains.plugins.github")

        testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        pluginVerifier()
    }

    testLibs(libs.test.mixin)
    testLibs(libs.test.spigotapi)
    testLibs(libs.test.bungeecord)
    testLibs(libs.test.spongeapi) {
        artifact {
            classifier = "shaded"
        }
    }
    testLibs(libs.test.fabricloader)
    testLibs(libs.test.nbt) {
        artifact {
            extension = "nbt"
        }
    }
    testLibs(projects.mixinTestData)

    // The IDE provides these at test time, the recorder runs outside it and needs its own
    "testSupportCompileOnly"(libs.kotlin.stdlib)
    "testSupportCompileOnly"(libs.gson)
    "testSupportCompileOnly"(libs.junit.api)
    "testSupportCompileOnly"(sourceSets.main.get().output)
    "testSupportImplementation"(libs.wiremock)
    testImplementation(testSupportSourceSet.output)
    testImplementation(libs.wiremock)
    httpFixtureRecorder(libs.kotlin.stdlib)
    httpFixtureRecorder(libs.gson)

    gradleToolingExtension(libs.gradleToolingExtension)
    gradleToolingExtension(libs.annotations)
}

changelog {
    version = coreVersion
    groups.empty()
    path = "changelog.md"
}

intellijPlatform {
    projectName = "Minecraft Development"

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks.patchPluginXml {
    val changelog = project.changelog
    changeNotes = changelog.render(Changelog.OutputType.HTML)
}

// Compile classes to be loaded into the Gradle VM to Java 8
// This is for maximum compatibility, these classes will be loaded into every Gradle import on all
// projects (not just Minecraft), so we don't want to break that with an incompatible class version.
tasks.named(gradleToolingExtensionSourceSet.compileJavaTaskName, JavaCompile::class) {
    options.release = 8
    options.compilerArgs = listOf("-Xlint:-options")
}

tasks.processResources {
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

// Run unit tests in paralllel. Unfortunately, to accomplish this, we also need separate sandboxes for each test fork.
// All of this is still worth doing since the IntelliJ test fixtures themselves are rather slow.
val testForks = 6
val sandboxTestTasks = mutableListOf<TaskProvider<PrepareSandboxTask>>()
repeat(testForks) {
    sandboxTestTasks += tasks.register<PrepareSandboxTask>("prepareTestSandboxFork$it") {
        description = "Prepare sandbox for test fork #$it"
        sandboxSuffix.set("-fork-$it")
        doFirst {
            sandboxDirectory.get().asFile.listFiles()
                ?.filter { f -> f.name.endsWith("-fork-$it") }
                ?.forEach { f -> f.deleteRecursively() }
        }
    }
}
tasks.prepareTestSandbox {
    doFirst {
        sandboxDirectory.get().asFile.listFiles()
            ?.filter { f -> f.name.endsWith("-test") }
            ?.forEach { f -> f.deleteRecursively() }
    }
}

val cleanTestSandboxForks = tasks.register("cleanTestSandboxForks", Delete::class) {
    description = "Delete all test fork sandboxes"
    doFirst {
        tasks.prepareTestSandbox.flatMap { it.sandboxDirectory }
            .get().asFile.listFiles()
            ?.filter { it.name.matches(Regex(".*-(?:fork-\\d+|test)")) }
            ?.let { delete(it) }
    }
}
tasks.test {
    dependsOn(tasks.jar, testLibs, sandboxTestTasks)
    finalizedBy(cleanTestSandboxForks)

    testLibs.resolvedConfiguration.resolvedArtifacts.forEach {
        systemProperty("testLibs.${it.name}", it.file.absolutePath)
    }
    systemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
    systemProperty("java.awt.headless", "true")

    dependsOn(builtinTemplatesHttpFixture)
    inputs.files(builtinTemplatesHttpFixture).withPropertyName("builtinTemplatesHttpFixture")
    systemProperty(
        "mcdev.test.httpFixtures",
        listOf(httpFixturesDir.asFile, generatedHttpFixturesDir.get().asFile).joinToString(File.pathSeparator),
    )
    // Outside HttpMockExtension, the plugin's requests go to a closed loopback port and fail with "connection refused"
    // instead of reaching the real services
    systemProperty("mcdev.test.httpMock", "http://127.0.0.1:1")

    val templatesDir = file("templates")
    inputs.files(fileTree(templatesDir) { exclude(".git") }).withPropertyName("templates")
    systemProperty("mcdev.test.templates", templatesDir.absolutePath)
    systemProperty("mcdev.test.templatesFormatVersion", builtinTemplatesFormatVersion)

    jvmArgs(
        "-Dsun.io.useCanonCaches=false",
        "-Dsun.io.useCanonPrefixCache=false",
    )

    val sandboxDir = tasks.prepareTestSandbox.flatMap { it.sandboxDirectory }.get().asFile

    maxParallelForks = testForks
    systemProperty("sandboxDir", sandboxDir.absolutePath)
    systemProperty("forks", testForks.toString())
}

idea {
    project.settings.taskTriggers.afterSync("generate")
}

license {
    val endings = listOf("java", "kt", "kts", "gradle.kts", "xml", "properties", "html", "flex", "bnf")
    exclude("META-INF/plugin.xml") // https://youtrack.jetbrains.com/issue/IDEA-345026
    exclude("sentry-debug-meta.properties", "sentry-external-modules.txt")
    include(endings.map { "**/*.$it" })

    val projectDir = layout.projectDirectory.asFile
    exclude {
        it.file.toRelativeString(projectDir)
            .replace("\\", "/")
            .startsWith("src/test/resources")
    }

    @Suppress("UnstableApiUsage")
    tasks {
        register("gradle") {
            files.from(
                fileTree(project.projectDir) {
                    include("*.gradle.kts", "gradle.properties")
                    exclude("**/buildSrc/**", "**/build/**")
                },
            )
        }
        register("buildSrc") {
            files.from(
                project.fileTree(project.projectDir.resolve("buildSrc")) {
                    include("**/*.kt", "**/*.kts")
                    exclude("**/build/**")
                },
            )
        }
        register("mixinTestData") {
            files.from(
                project.fileTree(project.projectDir.resolve("mixin-test-data")) {
                    include("**/*.java", "**/*.kts")
                    exclude("**/build/**")
                },
            )
        }
        register("grammars") {
            files.from(project.fileTree("src/main/grammars"))
        }
        register("externalAnnotations") {
            files.from(project.fileTree("externalAnnotations"))
        }
    }
}

val generateAtLexer = lexer("generateAtLexer", "AtLexer", "com/demonwav/mcdev/platform/minecraft/at/gen")
val generateAtParser = parser("generateAtParser", "AtParser", "com/demonwav/mcdev/platform/minecraft/at/gen")

val generateCtLexer = lexer("generateCtLexer", "CtLexer", "com/demonwav/mcdev/platform/minecraft/ct/gen")
val generateCtParser = parser("generateCtParser", "CtParser", "com/demonwav/mcdev/platform/minecraft/ct/gen")

val generateNbttLexer = lexer("generateNbttLexer", "NbttLexer", "com/demonwav/mcdev/nbt/lang/gen")
val generateNbttParser = parser("generateNbttParser", "NbttParser", "com/demonwav/mcdev/nbt/lang/gen")

val generateLangLexer = lexer("generateLangLexer", "LangLexer", "com/demonwav/mcdev/translations/lang/gen")
val generateLangParser = parser("generateLangParser", "LangParser", "com/demonwav/mcdev/translations/lang/gen")

val generateMEExpressionLexer = lexer("generateMEExpressionLexer", "MEExpressionLexer", "com/demonwav/mcdev/platform/mixin/expression/gen")
val generateMEExpressionParser = parser("generateMEExpressionParser", "MEExpressionParser", "com/demonwav/mcdev/platform/mixin/expression/gen")

val generateTranslationTemplateLexer = lexer(
    "generateTranslationTemplateLexer",
    "TranslationTemplateLexer",
    "com/demonwav/mcdev/translations/template/gen"
)

val generate = tasks.register("generate") {
    group = "minecraft"
    description = "Generate sources needed to compile the plugin."
    outputs.dir(layout.buildDirectory.dir("gen"))
    dependsOn(
        generateAtLexer,
        generateAtParser,
        generateCtLexer,
        generateCtParser,
        generateNbttLexer,
        generateNbttParser,
        generateLangLexer,
        generateLangParser,
        generateMEExpressionLexer,
        generateMEExpressionParser,
        generateTranslationTemplateLexer,
    )
}

sourceSets.main { java.srcDir(generate) }

// Remove gen directory on clean
tasks.clean { delete(generate) }

tasks.withType<PrepareSandboxTask> {
    pluginJar.set(tasks.jar.get().archiveFile)
    val pluginDirName = intellijPlatform.projectName.get()
    from(externalAnnotationsJar) {
        into("$pluginDirName/lib/resources")
    }
    from("templates") {
        exclude(".git")
        into("$pluginDirName/lib/resources/builtin-templates")
    }
}

tasks.runIde {
    maxHeapSize = "4G"

    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }

    // Kotlin K2 is enabled by default, uncomment to switch to K1
    // jvmArgumentProviders += CommandLineArgumentProvider {
    //     listOf("-Didea.kotlin.plugin.use.k2=false")
    // }

    // Set these properties to test different languages
    // systemProperty("user.language", "fr")
    // systemProperty("user.country", "FR")
}

if (System.getenv("CI") == "true" && System.getenv("NO_SENTRY") != "true") {
    configure<SentryPluginExtension> {
        includeSourceContext = true
        includeDependenciesReport = true
        autoInstallation {
            enabled = false
        }

        url = "https://sentry.mcdev.io/"
        org = "mcdev"
        projectName = "mcdev"
        authToken = providers.gradleProperty("mcdev.sentry.token")
    }

    // Wire together some tasks to make Gradle happy
    tasks.named("generateSentryBundleIdJava") {
        dependsOn(generate)
    }
    tasks.named("sentryCollectSourcesJava") {
        dependsOn(generate)
    }
    tasks.checkLicenseMain {
        dependsOn(tasks.named("generateSentryDebugMetaPropertiesjava"), tasks.named("collectExternalDependenciesForSentry"))
    }
}
