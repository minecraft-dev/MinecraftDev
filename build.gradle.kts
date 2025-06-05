/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import org.gradle.internal.jvm.Jvm
import org.jetbrains.changelog.Changelog
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    groovy
    id(libs.plugins.changelog.get().pluginId)
    alias(libs.plugins.idea.ext)
    `mcdev-core`
    `mcdev-parsing`
    `mcdev-publishing`
}

val coreVersion: String by project

val gradleToolingExtension: Configuration by configurations.creating
val testLibs: Configuration by configurations.creating {
    isTransitive = false
}

group = "com.demonwav.mcdev"

val gradleToolingExtensionSourceSet: SourceSet = sourceSets.create("gradle-tooling-extension") {
    configurations.named(compileOnlyConfigurationName) {
        extendsFrom(gradleToolingExtension)
    }
}
val gradleToolingExtensionJar = tasks.register<Jar>(gradleToolingExtensionSourceSet.jarTaskName) {
    from(gradleToolingExtensionSourceSet.output)
    archiveClassifier.set("gradle-tooling-extension")
    exclude("META-INF/plugin.xml")
}

val templatesSourceSet: SourceSet = sourceSets.create("templates") {
    resources {
        srcDir("templates")
        compileClasspath += sourceSets.main.get().output
    }
}

val templateSourceSets: List<SourceSet> = (file("templates").listFiles() ?: emptyArray()).mapNotNull { file ->
    if (file.isDirectory() && (file.listFiles() ?: emptyArray()).any { it.name.endsWith(".mcdev.template.json") }) {
        sourceSets.create("templates-${file.name}") {
            resources {
                srcDir(file)
                compileClasspath += sourceSets.main.get().output
            }
        }
    } else {
        null
    }
}

val externalAnnotationsJar = tasks.register<Jar>("externalAnnotationsJar") {
    from("externalAnnotations")
    destinationDirectory.set(layout.buildDirectory.dir("externalAnnotations"))
    archiveFileName.set("externalAnnotations.jar")
}

dependencies {
    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))

    implementation(files(gradleToolingExtensionJar))

    implementation(libs.mixinExtras.expressions) {
        exclude(group = "org.ow2.asm", module = "asm-debug-all")
    }
    testLibs(libs.mixinExtras.common)

    implementation(libs.mappingIo)
    implementation(libs.bundles.asm)

    implementation(libs.bundles.fuel) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellij.ide, useInstaller = false)

        // Bundled plugin dependencies
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("ByteCodeViewer")
        bundledPlugin("org.intellij.intelliLang")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("Git4Idea")
        bundledPlugin("com.intellij.modules.json")

        // Optional dependencies
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("org.toml.lang")
        bundledPlugin("org.jetbrains.plugins.yaml")


        testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        pluginVerifier()
    }

    testLibs(libs.test.mockJdk)
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

    // For non-SNAPSHOT versions (unless Jetbrains fixes this...) find the version with:
    // afterEvaluate { println(intellijPlatform.productInfo.buildNumber) }
    gradleToolingExtension(libs.groovy)
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

// Compile classes to be loaded into the Gradle VM to Java 5 to match Groovy
// This is for maximum compatibility, these classes will be loaded into every Gradle import on all
// projects (not just Minecraft), so we don't want to break that with an incompatible class version.
tasks.named(gradleToolingExtensionSourceSet.compileJavaTaskName, JavaCompile::class) {
    val java7Compiler = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(11)) }
    javaCompiler.set(java7Compiler)
    options.release.set(6)
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

    testLibs.resolvedConfiguration.resolvedArtifacts.forEach {
        systemProperty("testLibs.${it.name}", it.file.absolutePath)
    }
    systemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
    systemProperty("java.awt.headless", "true")

    jvmArgs(
        "-Dsun.io.useCanonCaches=false",
        "-Dsun.io.useCanonPrefixCache=false",
    )
}

idea {
    project.settings.taskTriggers.afterSync("generate")
}

license {
    val endings = listOf("java", "kt", "kts", "groovy", "gradle.kts", "xml", "properties", "html", "flex", "bnf")
    exclude("META-INF/plugin.xml") // https://youtrack.jetbrains.com/issue/IDEA-345026
    include(endings.map { "**/*.$it" })

    val projectDir = layout.projectDirectory.asFile
    exclude {
        it.file.toRelativeString(projectDir)
            .replace("\\", "/")
            .startsWith("src/test/resources")
    }

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

val generateAtLexer by lexer("AtLexer", "com/demonwav/mcdev/platform/mcp/at/gen")
val generateAtParser by parser("AtParser", "com/demonwav/mcdev/platform/mcp/at/gen")

val generateAwLexer by lexer("AwLexer", "com/demonwav/mcdev/platform/mcp/aw/gen")
val generateAwParser by parser("AwParser", "com/demonwav/mcdev/platform/mcp/aw/gen")

val generateNbttLexer by lexer("NbttLexer", "com/demonwav/mcdev/nbt/lang/gen")
val generateNbttParser by parser("NbttParser", "com/demonwav/mcdev/nbt/lang/gen")

val generateLangLexer by lexer("LangLexer", "com/demonwav/mcdev/translations/lang/gen")
val generateLangParser by parser("LangParser", "com/demonwav/mcdev/translations/lang/gen")

val generateMEExpressionLexer by lexer("MEExpressionLexer", "com/demonwav/mcdev/platform/mixin/expression/gen")
val generateMEExpressionParser by parser("MEExpressionParser", "com/demonwav/mcdev/platform/mixin/expression/gen")

val generateTranslationTemplateLexer by lexer(
    "TranslationTemplateLexer",
    "com/demonwav/mcdev/translations/template/gen"
)

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
