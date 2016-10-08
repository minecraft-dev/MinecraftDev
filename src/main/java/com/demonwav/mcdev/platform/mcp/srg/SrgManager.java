/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

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
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SrgManager {

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

                    final File temp = new File(System.getProperty("java.io.tmpdir"));
                    final File mcinit = new File(temp, "mcinit.gradle");

                    //noinspection ResultOfMethodCallIgnored
                    mcinit.delete();

                    final VirtualFile parent = buildGradle.getParent();
                    if (parent == null || parent.getCanonicalPath() == null) {
                        return;
                    }
                    final File dir = new File(parent.getCanonicalPath());

                    try {
                        unpackInitGradleFile(mcinit);
                    } catch (URISyntaxException | IOException e) {
                        e.printStackTrace();
                        currentPromise.setError("Could not create mcinit.gradle file");
                        return;
                    }

                    final GradleConnector connector = GradleConnector.newConnector();
                    connector.forProjectDirectory(dir);
                    ProjectConnection connection = null;

                    Set<File> files = null;
                    try {
                        connection = connector.connect();
                        final ModelBuilder<MinecraftDevModel> modelBuilder = connection.model(MinecraftDevModel.class);

                        Pair<String, Sdk> sdkPair = ExternalSystemJdkUtil.getAvailableJdk(module.getModule().getProject());
                        if (sdkPair != null && sdkPair.getSecond() != null && sdkPair.getSecond().getHomePath() != null &&
                            !ExternalSystemJdkUtil.USE_INTERNAL_JAVA.equals(sdkPair.getFirst())) {

                            modelBuilder.setJavaHome(new File(sdkPair.getSecond().getHomePath()));
                        }

                        modelBuilder.withArguments("--init-script", mcinit.getAbsolutePath());
                        final MinecraftDevModel model = modelBuilder.get();
                        files = model.getMappingFiles();
                    } catch (Exception e) {
                        e.printStackTrace();
                        currentPromise.setError("Gradle tooling could not be used to gather data");
                        return;
                    } finally {
                        if (connection != null) {
                            connection.close();
                        }
                    }

                    if (files == null) {
                        currentPromise.setError("Gradle tooling response was null");
                        return;
                    }

                    try {
                        currentMap = new SrgMap(files);
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentPromise.setError("SrgMap instance could not be created");
                        return;
                    }

                    currentPromise.setResult(currentMap);
                }
        }));
    }

    private void unpackInitGradleFile(@NotNull File mcinit) throws URISyntaxException, IOException {
        try (
            final InputStream gradleStream = getClass().getResourceAsStream("/mcinit.gradle");
            final FileOutputStream fileOutputStream = new FileOutputStream(mcinit);
            final FileChannel outChannel = fileOutputStream.getChannel();
            final ReadableByteChannel inChannel = Channels.newChannel(gradleStream)
        ) {
            outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE);
        }
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
            return Promises.rejectedPromise("Current SrgMap instance is null");
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
