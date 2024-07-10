/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

fun Project.lexer(flex: String, pack: String): TaskDelegate<JFlexExec> {
    configure<LicenseExtension> {
        exclude(pack.removeSuffix("/") + "/**")
    }

    return tasks.registering(JFlexExec::class) {
        sourceFile.set(layout.projectDirectory.file("src/main/grammars/$flex.flex"))
        destinationDirectory.set(layout.buildDirectory.dir("gen/$pack"))
        destinationFile.set(layout.buildDirectory.file("gen/$pack/$flex.java"))
        logFile.set(layout.buildDirectory.file("logs/generate$flex.log"))

        val jflex by project.configurations
        this.jflex.setFrom(jflex)

        val jflexSkeleton by project.configurations
        skeletonFile.set(jflexSkeleton.singleFile)
    }
}

fun Project.parser(bnf: String, pack: String): TaskDelegate<ParserExec> {
    configure<LicenseExtension> {
        exclude(pack.removeSuffix("/") + "/**")
    }

    return tasks.registering(ParserExec::class) {
        val destRoot = project.layout.buildDirectory.dir("gen")
        val dest = destRoot.map { it.dir(pack) }
        sourceFile.set(project.layout.projectDirectory.file("src/main/grammars/$bnf.bnf"))
        destinationRootDirectory.set(destRoot)
        destinationDirectory.set(dest)
        psiDirectory.set(dest.map { it.dir("psi") })
        parserDirectory.set(dest.map { it.dir("parser") })
        logFile.set(layout.buildDirectory.file("logs/generate$bnf.log"))

        val grammarKit by project.configurations
        this.grammarKit.setFrom(grammarKit)
    }
}

data class DepList(val intellijVersion: String, val intellijVersionName: String, val deps: List<Dep>)
data class Dep(val groupId: String, val artifactId: String, val version: String)
