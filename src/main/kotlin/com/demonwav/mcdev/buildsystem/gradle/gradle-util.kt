/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.gradle

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.io.File
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProgressListener

inline fun runGradleTask(
    project: Project?,
    dir: File,
    indicator: ProgressIndicator? = null,
    func: (BuildLauncher) -> Unit
) {
    val connector = GradleConnector.newConnector()
    connector.forProjectDirectory(dir)
    val connection = connector.connect()
    val launcher = connection.newBuild()

    connection.use {
        val sdkPair = ExternalSystemJdkUtil.getAvailableJdk(project)
        sdkPair.second?.homePath?.let { homePath ->
            if (ExternalSystemJdkUtil.USE_INTERNAL_JAVA != sdkPair.first) {
                launcher.setJavaHome(File(homePath))
            }
        }

        indicator?.let {
            launcher.addProgressListener(ProgressListener { event ->
                indicator.text = event.description
            })
        }

        func(launcher)
        launcher.run()
    }
}
