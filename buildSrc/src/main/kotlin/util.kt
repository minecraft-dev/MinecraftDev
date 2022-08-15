/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

import org.cadixdev.gradle.licenser.LicenseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.RegisteringDomainObjectDelegateProviderWithTypeAndAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.configure

typealias TaskDelegate<T> = RegisteringDomainObjectDelegateProviderWithTypeAndAction<out TaskContainer, T>

fun Project.lexer(flex: String, pack: String): TaskDelegate<JavaExec> {
    configure<LicenseExtension> {
        exclude(pack.removeSuffix("/") + "/**")
    }

    return tasks.registering(JavaExec::class) {
        val src = layout.projectDirectory.file("src/main/grammars/$flex.flex")
        val dst = layout.buildDirectory.dir("gen/$pack")
        val output = layout.buildDirectory.file("gen/$pack/$flex.java")

        val jflex by project.configurations
        val jflexSkeleton by project.configurations

        classpath = jflex
        mainClass.set("jflex.Main")

        doFirst {
            args(
                "--skel", jflexSkeleton.singleFile.absolutePath,
                "-d", dst.get().asFile.absolutePath,
                src.asFile.absolutePath
            )

            // Delete current lexer
            project.delete(output)
        }

        inputs.files(src, jflexSkeleton)
        outputs.file(output)
    }
}

fun Project.parser(bnf: String, pack: String): TaskDelegate<JavaExec> {
    configure<LicenseExtension> {
        exclude(pack.removeSuffix("/") + "/**")
    }

    return tasks.registering(JavaExec::class) {
        val src = project.layout.projectDirectory.file("src/main/grammars/$bnf.bnf")
        val dstRoot = project.layout.buildDirectory.dir("gen")
        val dst = dstRoot.map { it.dir(pack) }
        val psiDir = dst.map { it.dir("psi") }
        val parserDir = dst.map { it.dir("parser") }

        val grammarKit by project.configurations

        doFirst {
            project.delete(psiDir, parserDir)
        }

        classpath = grammarKit
        mainClass.set("org.intellij.grammar.Main")

        if (JavaVersion.current().isJava9Compatible) {
            jvmArgs(
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/java.util=ALL-UNNAMED"
            )
        }

        doFirst {
            args(dstRoot.get().asFile, src.asFile)
        }

        inputs.file(src)
        outputs.dirs(
            mapOf(
                "psi" to psiDir,
                "parser" to parserDir
            )
        )
    }
}

data class DepList(val intellijVersion: String, val intellijVersionName: String, val deps: List<Dep>)
data class Dep(val groupId: String, val artifactId: String, val version: String)
