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

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.io.path.absolute
import org.jetbrains.intellij.platform.gradle.utils.IdeServicesPluginRepositoryService
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.intellij.pluginRepository.model.ProductFamily

plugins {
    id("org.jetbrains.intellij.platform")
}

tasks.publishPlugin {
    properties["mcdev.deploy.token"]?.let { deployToken ->
        token.set(deployToken.toString())
    }
    channels.add(properties["mcdev.deploy.channel"]?.toString() ?: "Stable")

    // Overwrite the publish action, to properly set the until version after publishing (otherwise they ignore it).
    // See https://youtrack.jetbrains.com/issue/IJPL-166094/Plugins-Disable-until-build-range-check-by-default
    actions = listOf(Action {
        if (token.orNull.isNullOrEmpty()) {
            throw GradleException("No token specified for publishing. Make sure to specify mcdev.deploy.token.")
        }

        val log = Logging.getLogger(javaClass)

        val paths = archiveFiles.files.map { it.toPath() }
        val pluginId = "com.demonwav.minecraft-dev"
        paths.forEach { path ->
            channels.get().forEach { channel ->
                log.info("Uploading plugin '$pluginId' from '$path' to '${host.get()}', channel: '$channel'")

                try {
                    val repositoryClient = when (ideServices.get()) {
                        true -> PluginRepositoryFactory.createWithImplementationClass(
                            host.get(),
                            token.get(),
                            "Automation",
                            IdeServicesPluginRepositoryService::class.java,
                        )

                        false -> PluginRepositoryFactory.create(host.get(), token.get())
                    }
                    val uploadBean = repositoryClient.uploader.uploadUpdateByXmlIdAndFamily(
                        id = pluginId,
                        family = ProductFamily.INTELLIJ,
                        file = path.toFile(),
                        channel = channel.takeIf { it != "default" },
                        notes = null,
                        isHidden = hidden.get(),
                    )
                    log.info("Uploaded successfully as version ID ${uploadBean.id}")

                    val since = uploadBean.since
                    log.info("Since is ${since}, until is ${uploadBean.until}")
                    if (since != null && uploadBean.until.isNullOrBlank()) {
                        val newUntil = since.substringBefore(".") + ".*"
                        log.info("Updating until to $newUntil")
                        val request = HttpRequest.newBuilder()
                            .uri(URI.create("https://plugins.jetbrains.com/api/updates/${uploadBean.id}/since-until"))
                            .header("Authorization", "Bearer ${token.get()}")
                            .header("Content-Type", "application/json")
                            .header("User-Agent", "Minecraft Development Plugin Publisher")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"since\":\"${uploadBean.since}\",\"until\":\"$newUntil\"}"))
                            .build()
                        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw IOException("Updating until failed with status code ${response.statusCode()}, ${response.body()}")
                        }
                        log.info("Successful with status code ${response.statusCode()}")
                    }
                } catch (exception: Exception) {
                    throw GradleException("Failed to upload plugin: ${exception.message}", exception)
                }
            }
        }
    })
}
