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
import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

abstract class JFlexExec : JavaExec() {

    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:InputFiles
    abstract val jflex: ConfigurableFileCollection

    @get:InputFile
    abstract val skeletonFile: RegularFileProperty

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @get:OutputFile
    abstract val destinationFile: RegularFileProperty

    @get:Internal
    abstract val logFile: RegularFileProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    init {
        mainClass.set("jflex.Main")
    }

    override fun exec() {
        classpath = jflex

        args(
            "--skel", skeletonFile.get().asFile.absolutePath,
            "-d", destinationDirectory.get().asFile.absolutePath,
            sourceFile.get().asFile.absolutePath
        )

        fs.delete { delete(destinationDirectory) }

        val taskOutput = ByteArrayOutputStream()
        standardOutput = taskOutput
        errorOutput = taskOutput

        super.exec()

        val log = logFile.get().asFile
        log.parentFile.mkdirs()
        log.writeBytes(taskOutput.toByteArray())
    }
}
