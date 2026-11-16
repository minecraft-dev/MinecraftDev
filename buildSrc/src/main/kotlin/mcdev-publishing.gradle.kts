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

// Copied from PublishPluginTask, where it's private
fun Throwable.isIdeServicesUploadResponseParsingFailure(): Boolean {
    val message = message.orEmpty()
    return (javaClass.name == "com.fasterxml.jackson.databind.exc.InvalidFormatException" &&
        "Cannot deserialize value of type `int` from String" in message &&
        """PluginUpdateBean["id"]""" in message) ||
        cause?.isIdeServicesUploadResponseParsingFailure() == true ||
        suppressed.any { it.isIdeServicesUploadResponseParsingFailure() }
}

tasks.publishPlugin {
    (findProperty("mcdev.deploy.token") as String?)?.let { deployToken ->
        token.set(deployToken)
    }
    channels.add(findProperty("mcdev.deploy.channel")?.toString() ?: "Stable")

    // Overwrite the publish action, to properly set the until version after publishing (otherwise they ignore it).
    // See https://youtrack.jetbrains.com/issue/IJPL-166094/Plugins-Disable-until-build-range-check-by-default
    actions = listOf(Action {
        val resolvedToken = token.orNull
        if (resolvedToken.isNullOrEmpty()) {
            throw GradleException("No token specified for publishing. Make sure to specify mcdev.deploy.token.")
        }

        val log = Logging.getLogger(javaClass)

        val paths = archiveFiles.files.map { it.toPath().absolute() }
        val pluginId = "com.demonwav.minecraft-dev"

        val resolvedHost = host.get()
        val useIdeServices = ideServices.get()
        val isHidden = hidden.get()
        val resolvedChannels = channels.get()
        val repositoryClient = when (useIdeServices) {
            true -> PluginRepositoryFactory.createWithImplementationClass(
                resolvedHost,
                resolvedToken,
                "Automation",
                IdeServicesPluginRepositoryService::class.java,
            )

            false -> PluginRepositoryFactory.create(resolvedHost, resolvedToken)
        }

        paths.forEach { path ->
            resolvedChannels.forEach { channel ->
                log.info("Uploading plugin '$pluginId' from '$path' to '$resolvedHost', channel: '$channel'")

                val uploadBean = try {
                    repositoryClient.uploader.uploadUpdateByXmlIdAndFamily(
                        id = pluginId,
                        family = ProductFamily.INTELLIJ,
                        file = path.toFile(),
                        channel = channel.takeIf { it != "default" },
                        notes = null,
                        isHidden = isHidden,
                    )
                } catch (exception: Exception) {
                    if (useIdeServices && exception.isIdeServicesUploadResponseParsingFailure()) {
                        log.warn("Uploaded successfully; ignoring IDE Services upload response parsing failure.")
                        log.debug("IDE Services upload response parsing failure", exception)
                        // No upload response to read the version ID from, so the until update can't happen
                        return@forEach
                    }
                    throw GradleException("Failed to upload plugin: ${exception.message}", exception)
                }
                log.info("Uploaded successfully as version ID ${uploadBean.id}")

                try {
                    val since = uploadBean.since
                    log.info("Since is ${since}, until is ${uploadBean.until}")
                    if (since != null && uploadBean.until.isNullOrBlank()) {
                        val newUntil = since.substringBefore(".") + ".*"
                        log.info("Updating until to $newUntil")
                        val request = HttpRequest.newBuilder()
                            .uri(URI.create("https://plugins.jetbrains.com/api/updates/${uploadBean.id}/since-until"))
                            .header("Authorization", "Bearer $resolvedToken")
                            .header("Content-Type", "application/json")
                            .header("User-Agent", "Minecraft Development Plugin Publisher")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"since\":\"$since\",\"until\":\"$newUntil\"}"))
                            .build()
                        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw IOException("Updating until failed with status code ${response.statusCode()}, ${response.body()}")
                        }
                        log.info("Successful with status code ${response.statusCode()}")
                    }
                } catch (exception: Exception) {
                    throw GradleException("Failed to update until for plugin: ${exception.message}", exception)
                }
            }
        }
    })
}
