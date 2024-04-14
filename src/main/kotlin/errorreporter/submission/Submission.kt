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

package errorreporter.submission

@JvmRecord
data class Submission(
    val metadata: SubmissionMetadata,
    val errors: List<SubmissionContent>,
)

@JvmRecord
data class SubmissionMetadata(
    val pluginName: String,
    val pluginVersion: String,
    val osName: String,
    val javaVersion: String,
    val javaVmVendor: String,
    val isEap: Boolean,
    val appName: String,
    val ideaBuild: String,
    val ideaVersion: String,
    val lastAction: String?,
)

@JvmRecord
data class SubmissionContent(
    val message: String,
    val description: String?,
    val stacktrace: String,
    val attachments: List<SubmissionAttachment>,
)

@JvmRecord
data class SubmissionAttachment(val name: String, val displayText: String, val body: String?, val bytes: String?)
