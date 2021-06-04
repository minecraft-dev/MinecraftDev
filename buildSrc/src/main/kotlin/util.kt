/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

import java.io.File
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

fun Project.lexer(flex: String, pack: String) = tasks.registering(JavaExec::class) {
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

fun Project.parser(bnf: String, pack: String) = tasks.registering(JavaExec::class) {
    val src = "src/main/grammars/$bnf.bnf".replace('/', File.separatorChar)
    val dstRoot = "gen"
    val dst = "$dstRoot/$pack".replace('/', File.separatorChar)
    val psiDir = "$dst/psi/".replace('/', File.separatorChar)
    val parserDir = "$dst/parser/".replace('/', File.separatorChar)

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

    args(dstRoot, src)

    inputs.file(src)
    outputs.dirs(
        mapOf(
            "psi" to psiDir,
            "parser" to parserDir
        )
    )
}
