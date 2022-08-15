/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

abstract class Filter : TransformAction<Filter.Params> {
    interface Params : TransformParameters {
        @get:Input
        val ideaVersion: Property<String>
        @get:Input
        val ideaVersionName: Property<String>
        @get:PathSensitive(PathSensitivity.NONE)
        @get:InputFile
        val depsFile: RegularFileProperty
    }

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    abstract val layout: ProjectLayout

    private val deps: List<Dep>?

    init {
        deps = run {
            val depsFile = parameters.depsFile.orNull?.asFile ?: return@run null
            if (!depsFile.exists()) {
                return@run null
            }

            val depList: DepList = depsFile.bufferedReader().use { reader ->
                Gson().fromJson(reader, DepList::class.java)
            }

            if (
                parameters.ideaVersion.orNull == depList.intellijVersion &&
                parameters.ideaVersionName.orNull == depList.intellijVersionName
            ) {
                depList.deps
            } else {
                null
            }
        }
    }

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile.toPath()

        // exclude the coroutines jar
        // We include our own - but also IntelliJ's jar breaks sources
        val inputParts = input.map { it.toString() }
        if (!inputParts.containsAll(pathParts)) {
            outputs.file(inputArtifact)
            return
        }

        val fileName = inputParts.last()
        if (fileName.startsWith("kotlinx-coroutines")) {
            return
        }

        deps?.forEach { d ->
            if (fileName == "${d.artifactId}-${d.version}.jar") {
                return
            }
        }

        outputs.file(inputArtifact)
    }

    companion object {
        private val pathParts = listOf("com.jetbrains.intellij.idea", "ideaIC", "lib")
    }
}
