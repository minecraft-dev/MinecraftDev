package com.demonwav.mcdev.platform.mcp.srg;

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.platform.mcp.McpModule;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class SrgManager {
    private static final String TASK_NAME = "___getMinecraftDevForIntelliJData____";
    private static final String FILE_NAME_BASE = ".tmpMCDVIJ";

    private static final ConcurrentHashMap<McpModule, SrgManager> managers = new ConcurrentHashMap<>();

    public static SrgManager getInstance(@NotNull McpModule module) {
        return managers.computeIfAbsent(module, SrgManager::new);
    }

    private final Object lock = new Object();

    private final McpModule module;
    private AsyncPromise<SrgMap> currentPromise;
    private SrgMap currentMap;

    private SrgManager(@NotNull McpModule module) {
        this.module = module;
    }

    /**
     * Find the current location of the srg mappings and reset the {@link SrgMap} instance this class holds.
     * This is a long operation, so there may be significant delay from when the job is requested to when it finishes.
     * <p>
     * This may be called from any thread.
     */
    public void recomputeSrgMap() {
        synchronized (lock) {
            if (currentPromise != null) {
                return;
            }

            currentPromise = new AsyncPromise<>();
        }

        ApplicationManager.getApplication().invokeLater(() ->
            ProgressManager.getInstance().run(new Task.Backgroundable(module.getModule().getProject(), "Gathering MCP Data", true) {
                @Override
                public boolean shouldStartInBackground() {
                    return false;
                }

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    final GradleBuildSystem buildSystem = module.getBuildSystem();
                    if (buildSystem == null) {
                        currentPromise.setError("Module BuildSystem is null");
                        return;
                    }

                    final VirtualFile buildGradle = buildSystem.getBuildGradle();

                    if (buildGradle == null) {
                        currentPromise.setError("build.gradle file could not be found");
                        return;
                    }

                    final String canonicalPath = buildGradle.getCanonicalPath();
                    if (canonicalPath == null) {
                        return;
                    }

                    File buildGradleFile = new File(canonicalPath);

                    final String text;
                    try {
                        text = new String(Files.readAllBytes(buildGradleFile.toPath()), buildGradle.getCharset());
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentPromise.setError("build.gradle file could not be read");
                        return;
                    }

                    final File dir = buildGradleFile.getParentFile();

                    final int r = new Random().nextInt();

                    final String appended = text +
                        "\n\ntask " + TASK_NAME + " {file(\"" + FILE_NAME_BASE + r + "\") << project.tasks.genSrgs.mcpToSrg}";

                    try {
                        Files.write(buildGradleFile.toPath(), appended.getBytes(buildGradle.getCharset()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentPromise.setError("Temporary build.gradle could not be written");
                        return;
                    }

                    GradleConnector connector = GradleConnector.newConnector();
                    connector.forProjectDirectory(dir);
                    ProjectConnection connection = connector.connect();
                    BuildLauncher launcher = connection.newBuild();

                    try {
                        Pair<String, Sdk> sdkPair = ExternalSystemJdkUtil.getAvailableJdk(module.getModule().getProject());
                        if (sdkPair != null && sdkPair.getSecond() != null && sdkPair.getSecond().getHomePath() != null &&
                            !ExternalSystemJdkUtil.USE_INTERNAL_JAVA.equals(sdkPair.getFirst())) {

                            launcher.setJavaHome(new File(sdkPair.getSecond().getHomePath()));
                        }

                        launcher.forTasks(TASK_NAME).run();
                    } catch (Exception e) {
                        try {
                            Files.write(buildGradleFile.toPath(), text.getBytes(buildGradle.getCharset()));
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        currentPromise.setError("Gradle build could not be run to gather data");
                        return;
                    } finally {
                        connection.close();
                    }

                    try {
                        Files.write(buildGradleFile.toPath(), text.getBytes(buildGradle.getCharset()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    final String path;
                    final File file = new File(dir, FILE_NAME_BASE + r);
                    try {
                        path = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentPromise.setError("Path could not be read from file");
                        return;
                    } finally {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }

                    try {
                        currentMap = new SrgMap(new File(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentPromise.setError("SrgMap instance could not be created");
                        return;
                    }

                    currentPromise.setResult(currentMap);
                }
        }));
    }

    /**
     * Returns a {@link Promise} of an instance of an immutable {@link SrgMap}. The SrgMap instance may be used to make queries on different
     * mappings. The promise may in most cases complete immediately, but if {@link #recomputeSrgMap()} is running, this will not complete
     * until that operation is finished.
     * <p>
     * If {@link #recomputeSrgMap()} has not yet been run, this will return a rejected promise.
     * <p>
     * This may be called from any thread.
     *
     * @return A {@link Promise} containing a {@link SrgMap}.
     */
    public Promise<SrgMap> getSrgMap() {
        synchronized (lock) {
            if (currentPromise != null) {
                return currentPromise;
            }
            if (currentMap != null) {
                return Promise.resolve(currentMap);
            }
            return Promise.reject("Current SrgMap instance is null");
        }
    }

    /**
     * Returns the current {@link SrgMap} instance if it is availble. Returns null if it is not available. Returns immediately in all cases.
     * <p>
     * This may be called from any thread.
     *
     * @return The current {@link SrgMap} instance, if avaialble.
     */
    @Nullable
    public SrgMap getSrgMapNow() {
        return currentMap;
    }

    /**
     * Returns a {@link Promise} of an instance of an immutable {@link SrgMap}. Ths SrgMap instance may be used to make queries on different
     * mappings. the promise may in most cases complete immediately, but if {@link #recomputeSrgMap()} is running or will be ran by this
     * method, this will not complete until that operation is finished.
     * <p>
     * If {@link #recomputeSrgMap()} has not yet been run, this will run it and return the promise to be fulfilled when it's finished.
     * <p>
     * This may be called from any thread.
     *
     * @return A {@link Promise} containing a {@link SrgMap}.
     */
    public Promise<SrgMap> recomputeIfNullAndGetSrgMap() {
        synchronized (lock) {
            if (currentMap != null) {
                return Promise.resolve(currentMap);
            }

            if (currentPromise != null) {
                return currentPromise;
            }
            recomputeSrgMap();
            return currentPromise;
        }
    }
}
