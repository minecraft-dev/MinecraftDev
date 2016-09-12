import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.intellij.IntelliJPluginExtension
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
    }
}

val ideaVersion by project
val javaVersion by project
val pluginVersion by project
val downloadIdeaSources by project

apply {
    plugin<JavaPlugin>()
    plugin<ScalaPlugin>()
    plugin("kotlin")
    plugin<IdeaPlugin>()
    plugin("org.jetbrains.intellij")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = javaVersion as String
    targetCompatibility = javaVersion as String
}

group = "com.demonwav.minecraft-dev"
version = pluginVersion

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile("org.scala-lang:scala-library:2.11.8")
    compile(kotlinModule("stdlib-jre8"))
}

var intellijSandboxDirectory: String? = null
configure<IntelliJPluginExtension> {
    // IntelliJ IDEA dependency
    version =  if (project.hasProperty("intellijVersion")){
        project.properties["intellijVersion"] as String
    } else {
        ideaVersion as String
    }
    // Bundled plugin dependencies
    setPlugins("maven", "gradle", "Groovy", "yaml", "Kotlin", "org.intellij.scala:2016.2.0")

    pluginName = "Minecraft Development"
    updateSinceUntilBuild = false

    downloadSources = (downloadIdeaSources as String).toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
    intellijSandboxDirectory = sandboxDirectory
}

configure<IdeaModel> {
    project.apply {
        jdkName = "1.8"
        setLanguageLevel("1.8")
    }

    module.apply {
        excludeDirs.add(File(".sandbox"))
    }
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

afterEvaluate {
    val buildPlugin = getTasksByName("buildPlugin", false)
    val runIdea = getTasksByName("runIdea", false)

    buildPlugin.forEach { it.dependsOn.addAll(listOf(initPropTask)) }
    runIdea.forEach { it.dependsOn.addAll(listOf(initPropTask)) }
}

defaultTasks("buildPlugin")
