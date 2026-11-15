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

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering

fun Project.lexer(name: String, flex: String, pack: String): TaskProvider<JFlexExec> {
    extensions.configure<PatternFilterable>("license") {
        exclude(pack.removeSuffix("/") + "/**")
    }

    return tasks.register(name, JFlexExec::class) {
        sourceFile.set(layout.projectDirectory.file("src/main/grammars/$flex.flex"))
        destinationDirectory.set(layout.buildDirectory.dir("gen/$pack/lexer"))
        destinationFile.set(layout.buildDirectory.file("gen/$pack/lexer/$flex.java"))
        logFile.set(layout.buildDirectory.file("logs/generate$flex.log"))

        this.jflex.setFrom(project.configurations.named("jflex"))

        skeletonFile.set(project.configurations.named("jflexSkeleton").get().singleFile)
    }
}

fun Project.parser(name: String, bnf: String, pack: String): TaskProvider<ParserExec> {
    extensions.configure<PatternFilterable>("license") {
        exclude(pack.removeSuffix("/") + "/**")
    }

    return tasks.register(name, ParserExec::class) {
        val destRoot = project.layout.buildDirectory.dir("gen")
        val dest = destRoot.map { it.dir(pack) }
        sourceFile.set(project.layout.projectDirectory.file("src/main/grammars/$bnf.bnf"))
        destinationRootDirectory.set(destRoot)
        psiDirectory.set(dest.map { it.dir("psi") })
        parserDirectory.set(dest.map { it.dir("parser") })
        logFile.set(layout.buildDirectory.file("logs/generate$bnf.log"))

        this.grammarKit.setFrom(project.configurations.named("grammarKit"))
    }
}

fun Project.git(vararg args: String): String {
    val output = providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }
    return output.standardOutput.asText.get()
}
