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

package com.demonwav.mcdev.creator.custom.npm

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.update.PluginUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.DigestUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.URLUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A plugin-maintained npm environment.
 *
 * Packages are installed with `npm install --prefix <dir>`, which keeps them in `<dir>/node_modules` instead of the
 * user's global npm installation, and commands are run with `npm exec --prefix <dir>`, which puts the installed
 * packages' executables on the PATH. Packages are updated every time they are run.
 *
 * Packages can also come from git repositories, which don't need to be published to npm. They are cloned once and
 * pulled every time they are run, then their dependencies are installed and their build scripts are run whenever the
 * pull changed anything, and their files are run directly with Node.js.
 */
object NpmEnvironment {

    private val LOGGER = logger<NpmEnvironment>()
    private val TIMEOUT = 10.minutes
    private val ANSI_ESCAPE = Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]")

    // Serializes all access to the environment, so concurrent project creations don't corrupt node_modules
    private val mutex = Mutex()

    private const val CUSTOM_NPM_PATH_KEY = "mcdev.creator.npm.path"
    private const val REGISTRY_URL = "https://registry.npmjs.org"

    val directory: Path
        get() = PathManager.getSystemDir().resolve("mcdev-npm")

    /**
     * The path to the npm executable chosen by the user, or an empty string to look it up in the PATH.
     */
    var customNpmPath: String
        get() = PropertiesComponent.getInstance().getValue(CUSTOM_NPM_PATH_KEY, "")
        set(value) = PropertiesComponent.getInstance().setValue(CUSTOM_NPM_PATH_KEY, value.trim(), "")

    fun findNpm(customPath: String = customNpmPath): String? {
        if (customPath.isNotBlank()) {
            val file = runCatching { Path.of(customPath.trim()) }.getOrNull()
                ?: return null
            return file.toAbsolutePath().toString().takeIf { file.isRegularFile() && file.isExecutable() }
        }

        val name = if (SystemInfo.isWindows) "npm.cmd" else "npm"
        return PathEnvironmentVariableUtil.findFirst(name)?.toAbsolutePath()?.toString()
    }

    /**
     * Fetches information about who published the given version (or dist-tag) of a package from the npm registry.
     */
    fun fetchPackageInfo(source: NpmPackageSource.Registry): NpmPackageInfo {
        // Scoped packages need their slash to be escaped
        val url = "$REGISTRY_URL/${source.name.replace("/", "%2F")}/${URLUtil.encodeURIComponent(source.version)}"
        val text = HttpRequests.request(url)
            .userAgent(PluginUtil.useragent)
            .readString()
        val json = Json.parseToJsonElement(text).jsonObject

        fun JsonElement?.personName(): String? = when (this) {
            is JsonObject -> this["name"]?.jsonPrimitive?.contentOrNull
            is JsonPrimitive -> contentOrNull?.substringBefore('<')?.trim()
            else -> null
        }

        val repository = when (val repo = json["repository"]) {
            is JsonObject -> repo["url"]?.jsonPrimitive?.contentOrNull
            is JsonPrimitive -> repo.contentOrNull
            else -> null
        }

        return NpmPackageInfo(
            version = json["version"]?.jsonPrimitive?.contentOrNull,
            publisher = json["_npmUser"].personName(),
            maintainers = (json["maintainers"] as? JsonArray).orEmpty().mapNotNull { it.personName() },
            repository = repository?.removePrefix("git+")?.removeSuffix(".git"),
        )
    }

    /**
     * Installs or updates the given package in the environment, then runs it.
     *
     * If a package from the registry cannot be updated, for example because the registry can't be reached, the
     * previously installed version is used if there is one.
     *
     * @return `true` on success, otherwise an error notification is shown to the user and `false` is returned
     */
    suspend fun runPackage(
        project: Project,
        source: NpmPackageSource,
        entryPoint: NpmEntryPoint,
        args: List<String>,
        workingDirectory: Path,
        input: String?,
    ): Boolean {
        val npm = findNpmOrNotify(project) ?: return false
        return mutex.withLock {
            val commandLine = when (source) {
                is NpmPackageSource.Registry -> prepareRegistryPackage(project, npm, source, entryPoint)
                is NpmPackageSource.Git -> prepareGitPackage(project, npm, source, entryPoint)
            } ?: return@withLock false

            commandLine.withParameters(args).withWorkingDirectory(workingDirectory)
            val output = run(commandLine, input)
            if (!output.isSuccess) {
                notifyFailure(project, commandLine, output, NotificationType.ERROR)
                return@withLock false
            }
            true
        }
    }

    private suspend fun prepareRegistryPackage(
        project: Project,
        npm: String,
        source: NpmPackageSource.Registry,
        entryPoint: NpmEntryPoint,
    ): GeneralCommandLine? {
        val packageDir = directory.resolve("node_modules").resolve(source.name).normalize()
        if (!packageDir.startsWith(directory)) {
            notifyError(project, MCDevBundle("creator.npm.invalid_package.content", source.name))
            return null
        }

        if (!install(project, npm, source, packageDir)) {
            return null
        }

        return when (entryPoint) {
            is NpmEntryPoint.Command -> {
                // Make sure the command comes from the package the user was told about, and not some other package
                if (entryPoint.name !in findPackageBinaries(packageDir)) {
                    notifyCommandNotFound(project, entryPoint, source)
                    return null
                }

                // --no prevents npm from looking for the command in the registry if it's missing from the
                // environment, which would download whatever package has the same name as the command, into npm's
                // shared cache
                GeneralCommandLine(npm, "exec", "--prefix", directory.toString(), "--no", "--", entryPoint.name)
            }
            is NpmEntryPoint.Script -> createNodeCommandLine(project, npm, packageDir, entryPoint.path, source)
        }
    }

    private suspend fun install(
        project: Project,
        npm: String,
        source: NpmPackageSource.Registry,
        packageDir: Path,
    ): Boolean {
        directory.createDirectories()
        val commandLine = GeneralCommandLine(
            npm,
            "install",
            "--prefix",
            directory.toString(),
            "--no-audit",
            "--no-fund",
            "${source.name}@${source.version}",
        ).withWorkingDirectory(directory)
        val output = run(commandLine, null)
        if (output.isSuccess) {
            return true
        }

        if (!packageDir.resolve("package.json").isRegularFile()) {
            notifyFailure(project, commandLine, output, NotificationType.ERROR)
            return false
        }

        // Not being able to update isn't fatal, the user can still create their project with the version they have
        notifyFailure(project, commandLine, output, NotificationType.WARNING)
        return true
    }

    private suspend fun prepareGitPackage(
        project: Project,
        npm: String,
        source: NpmPackageSource.Git,
        entryPoint: NpmEntryPoint,
    ): GeneralCommandLine? {
        val checkout = cloneAndBuild(project, npm, source) ?: return null
        val script = when (entryPoint) {
            // The package isn't installed in an environment npm exec could use, run its binary directly
            is NpmEntryPoint.Command -> findPackageBinaries(checkout)[entryPoint.name] ?: run {
                notifyCommandNotFound(project, entryPoint, source)
                return null
            }
            is NpmEntryPoint.Script -> entryPoint.path
        }
        return createNodeCommandLine(project, npm, checkout, script, source)
    }

    /**
     * Clones the repository, or pulls it if it was already built, then installs its dependencies and runs its build
     * scripts if anything changed since the last build.
     *
     * If the repository cannot be pulled, for example because the user is offline, the last build is used as is.
     *
     * @return the directory of the built package, or `null` if it could not be built
     */
    private suspend fun cloneAndBuild(project: Project, npm: String, source: NpmPackageSource.Git): Path? {
        val checkout = directory.resolve("git").resolve(DigestUtil.sha256Hex(source.url.toByteArray()).take(16))
        // Holds the commit that was last built successfully. It's in .git so that it can't conflict with any pull
        val builtMarker = checkout.resolve(".git").resolve("mcdev-built")
        val builtCommit = runCatching { builtMarker.readText().trim() }.getOrNull()

        if (builtCommit != null) {
            val pull = GeneralCommandLine("git", "pull", "--ff-only").withWorkingDirectory(checkout)
            val pullOutput = runGit(project, pull) ?: return null
            if (!pullOutput.isSuccess) {
                // Not being able to update isn't fatal, the user can still create their project with their last build
                notifyFailure(project, pull, pullOutput, NotificationType.WARNING)
                return checkout
            }
        } else {
            // Either it was never cloned, or the last clone or build failed, start over in case it left a broken state
            runInterruptible(Dispatchers.IO) { NioFiles.deleteRecursively(checkout) }
            val clone = GeneralCommandLine("git", "clone", "--depth", "1", "--", source.url, checkout.toString())
            val cloneOutput = runGit(project, clone) ?: return null
            if (!cloneOutput.isSuccess) {
                notifyFailure(project, clone, cloneOutput, NotificationType.ERROR)
                return null
            }
        }

        val revParse = GeneralCommandLine("git", "rev-parse", "HEAD").withWorkingDirectory(checkout)
        val revParseOutput = runGit(project, revParse) ?: return null
        if (!revParseOutput.isSuccess) {
            notifyFailure(project, revParse, revParseOutput, NotificationType.ERROR)
            return null
        }
        val commit = revParseOutput.stdout.trim()
        if (commit == builtCommit) {
            return checkout
        }

        // Make sure an interrupted or failed build is never mistaken for a successful one
        runInterruptible(Dispatchers.IO) { Files.deleteIfExists(builtMarker) }

        // Building usually needs the dev dependencies too, which both commands install
        val install = if (checkout.resolve("package-lock.json").isRegularFile()) "ci" else "install"
        val commandLines = listOf(GeneralCommandLine(npm, install, "--no-audit", "--no-fund")) +
            source.buildScripts.map { GeneralCommandLine(npm, "run", it) }
        for (commandLine in commandLines) {
            val output = run(commandLine.withWorkingDirectory(checkout), null)
            if (!output.isSuccess) {
                notifyFailure(project, commandLine, output, NotificationType.ERROR)
                return null
            }
        }

        builtMarker.writeText(commit)
        return checkout
    }

    /**
     * Creates a command line running a JavaScript file of a package with Node.js.
     */
    private fun createNodeCommandLine(
        project: Project,
        npm: String,
        packageDir: Path,
        script: String,
        source: NpmPackageSource,
    ): GeneralCommandLine? {
        val file = packageDir.resolve(script).normalize()
        if (!file.startsWith(packageDir) || !file.isRegularFile()) {
            val content = MCDevBundle(
                "creator.npm.script_not_found.content",
                StringUtil.escapeXmlEntities(script),
                StringUtil.escapeXmlEntities(source.displayName),
            )
            notifyError(project, content)
            return null
        }

        val node = findNode(npm) ?: run {
            notifyError(project, MCDevBundle("creator.npm.node_not_found.content"))
            return null
        }
        return GeneralCommandLine(node, file.toString())
    }

    private fun findNode(npm: String): String? {
        // npm is normally installed alongside the Node.js it comes with
        val name = if (SystemInfo.isWindows) "node.exe" else "node"
        val sibling = Path.of(npm).resolveSibling(name)
        if (sibling.isRegularFile() && sibling.isExecutable()) {
            return sibling.toString()
        }
        return PathEnvironmentVariableUtil.findFirst(name)?.toAbsolutePath()?.toString()
    }

    /**
     * @return the package's binaries, mapped to their files relative to the package directory
     */
    private fun findPackageBinaries(packageDir: Path): Map<String, String> {
        val packageJson = packageDir.resolve("package.json")
        if (!packageJson.isRegularFile()) {
            return emptyMap()
        }

        val json = runCatching { Json.parseToJsonElement(packageJson.readText()).jsonObject }
            .onFailure { LOGGER.warn("Failed to read $packageJson", it) }
            .getOrNull()
            ?: return emptyMap()
        return when (val bin = json["bin"]) {
            // A single binary is named after the package, without its scope
            is JsonPrimitive -> {
                val name = json["name"]?.jsonPrimitive?.contentOrNull?.substringAfterLast('/')
                    ?: return emptyMap()
                mapOf(name to bin.content)
            }
            is JsonObject -> bin.mapNotNull { (name, file) -> (file as? JsonPrimitive)?.let { name to it.content } }
                .toMap()
            else -> emptyMap()
        }
    }

    private fun notifyCommandNotFound(project: Project, entryPoint: NpmEntryPoint.Command, source: NpmPackageSource) {
        val content = MCDevBundle(
            "creator.npm.command_not_in_package.content",
            StringUtil.escapeXmlEntities(entryPoint.name),
            StringUtil.escapeXmlEntities(source.displayName),
        )
        notifyError(project, content)
    }

    /**
     * @return the output of the git command, or `null` if git could not be started
     */
    private suspend fun runGit(project: Project, commandLine: GeneralCommandLine): ProcessOutput? =
        try {
            run(commandLine, null)
        } catch (e: ExecutionException) {
            LOGGER.warn("Failed to run ${commandLine.commandLineString}", e)
            notifyError(project, MCDevBundle("creator.npm.git_not_found.content"))
            null
        }

    private suspend fun run(commandLine: GeneralCommandLine, input: String?): ProcessOutput {
        LOGGER.info("Running ${commandLine.commandLineString}")
        return runInterruptible(Dispatchers.IO) {
            val handler = CapturingProcessHandler(commandLine)
            handler.processInput.use { stdin ->
                if (input != null) {
                    stdin.write(input.toByteArray(commandLine.charset))
                }
            }
            handler.runProcess(TIMEOUT.inWholeMilliseconds.toInt())
        }
    }

    private val ProcessOutput.isSuccess: Boolean
        get() = exitCode == 0 && !isTimeout && !isCancelled

    private fun notifyFailure(
        project: Project,
        commandLine: GeneralCommandLine,
        output: ProcessOutput,
        type: NotificationType,
    ) {
        LOGGER.warn(
            "Command ${commandLine.commandLineString} failed with exit code ${output.exitCode}" +
                " (timeout = ${output.isTimeout})\nstdout:\n${output.stdout}\nstderr:\n${output.stderr}"
        )

        val details = output.stderr.ifBlank { output.stdout }
            .replace(ANSI_ESCAPE, "")
            .lines()
            .filter { it.isNotBlank() }
            .takeLast(10)
            .joinToString("<br>") { StringUtil.escapeXmlEntities(it) }
        val content = MCDevBundle(
            "creator.npm.command_failed.content",
            StringUtil.escapeXmlEntities(commandLine.commandLineString),
            output.exitCode,
            details,
        )
        notifyProblem(project, content, type)
    }

    private fun notifyProblem(project: Project, content: String, type: NotificationType) {
        if (type == NotificationType.ERROR) {
            notifyError(project, content)
        } else {
            notify(project, MCDevBundle("creator.npm.update_failed.title"), content, type)
        }
    }

    private fun findNpmOrNotify(project: Project): String? {
        val npm = findNpm()
        if (npm == null) {
            notifyError(project, MCDevBundle("creator.npm.not_found.content"))
        }
        return npm
    }

    private fun notifyError(project: Project, content: String) =
        notify(project, MCDevBundle("creator.npm.error.title"), content, NotificationType.ERROR)

    private fun notify(project: Project, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("Minecraft project creator")
            .createNotification(title, content, type)
            .notify(project)
    }
}

/**
 * Where a package comes from.
 */
sealed interface NpmPackageSource {
    val displayName: String

    /**
     * A package published to the npm registry.
     *
     * @param version an exact version or dist-tag
     */
    data class Registry(val name: String, val version: String) : NpmPackageSource {
        override val displayName get() = name
    }

    /**
     * A package at the root of a git repository, which is cloned and built from source.
     *
     * @param buildScripts the scripts from its `package.json` to run once its dependencies are installed
     */
    data class Git(val url: String, val buildScripts: List<String>) : NpmPackageSource {
        override val displayName get() = url
    }
}

/**
 * What to run from a package.
 */
sealed interface NpmEntryPoint {
    val displayName: String

    /**
     * One of the executables declared in the package's `bin`.
     */
    data class Command(val name: String) : NpmEntryPoint {
        override val displayName get() = name
    }

    /**
     * A JavaScript file of the package, relative to its root, run with Node.js.
     */
    data class Script(val path: String) : NpmEntryPoint {
        override val displayName get() = path
    }
}

data class NpmPackageInfo(
    val version: String?,
    val publisher: String?,
    val maintainers: List<String>,
    val repository: String?,
)
