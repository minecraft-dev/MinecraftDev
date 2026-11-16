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

plugins {
    idea
}

val jflex = configurations.create("jflex")
val jflexSkeleton = configurations.create("jflexSkeleton")
val grammarKit = configurations.create("grammarKit")

val libs = versionCatalogs.named("libs")
dependencies {
    jflex(libs.findLibrary("jflex.lib").get())
    jflexSkeleton(libs.findLibrary("jflex.skeleton").get()) {
        artifact {
            extension = "skeleton"
        }
    }
    grammarKit(libs.findLibrary("grammarKit").get())
}

idea {
    module {
        generatedSourceDirs.add(file("build/gen"))
    }
}
