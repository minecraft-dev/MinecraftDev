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

package com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom;

import com.demonwav.mcdev.platform.mcp.gradle.tooling.ReflectUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;

public final class FabricLoomModelBuilderImpl extends AbstractModelBuilderService {

    @Override
    public boolean canBuild(String modelName) {
        return FabricLoomModel.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(@NotNull String modelName, @NotNull Project project, @NotNull ModelBuilderContext context) {
        if (!project.getPlugins().hasPlugin("fabric-loom")) {
            return null;
        }

        Object loomExtension = project.getExtensions().getByName("loom");

        try {
            return build(project, loomExtension);
        } catch (Exception ex) {
            //noinspection UnstableApiUsage
            context.getMessageReporter().createMessage()
                .withGroup("com.demonwav.mcdev")
                .withTitle("Minecraft Dev: Loom import error")
                .withText("An error occurred while importing Loom data, falling back to legacy import")
                    .withKind(Message.Kind.WARNING)
                    .withStackTrace()
                    .withException(ex)
                    .reportMessage(project);

            try {
                // Must be using an older loom version, fallback.
                return buildLegacy(project, loomExtension);
            } catch (Exception fallbackEx) {
                // Return null if even legacy fallback fails
                return null;
            }
        }
    }

    private FabricLoomModel build(Project project, Object loomExtension) throws Exception {
        Object minecraftProvider = ReflectUtil.getProperty(loomExtension, "minecraftProvider");
        String minecraftVersion = (String) ReflectUtil.callMethod(minecraftProvider, "minecraftVersion");

        File tinyMappings = null;
        if (ReflectUtil.hasProperty(loomExtension, "mappingsFile")) {
            Object mappingsFileObj = ReflectUtil.getProperty(loomExtension, "mappingsFile");
            if (mappingsFileObj instanceof Provider) {
                mappingsFileObj = ((Provider<?>) mappingsFileObj).getOrNull();
            }
            if (mappingsFileObj instanceof File) {
                tinyMappings = (File) mappingsFileObj;
            } else if (mappingsFileObj != null) {
                tinyMappings = project.file(mappingsFileObj);
            }
        }

        boolean splitMinecraftJar = (Boolean) ReflectUtil.callMethod(loomExtension, "areEnvironmentSourceSetsSplit");

        Map<String, List<FabricLoomModel.DecompilerModel>> decompilers = new HashMap<>();

        if (splitMinecraftJar) {
            decompilers.put("common", getDecompilers(loomExtension, false));
            decompilers.put("client", getDecompilers(loomExtension, true));
        } else {
            decompilers.put("single", getDecompilers(loomExtension, false));
        }

        return new FabricLoomModelImpl(minecraftVersion, tinyMappings, decompilers, splitMinecraftJar);
    }

    private List<FabricLoomModel.DecompilerModel> getDecompilers(Object loomExtension, boolean client) throws Exception {
        Object decompilerOptions = ReflectUtil.getProperty(loomExtension, "decompilerOptions");
        List<FabricLoomModel.DecompilerModel> result = new ArrayList<>();
        if (decompilerOptions instanceof Iterable) {
            for (Object option : (Iterable<?>) decompilerOptions) {
                Object task = ReflectUtil.callMethod(loomExtension, "getDecompileTask", option, client);
                String sourcesPath;
                if (ReflectUtil.hasProperty(task, "outputJar")) {
                    // Pre 1.8
                    Object outputJar = ReflectUtil.getProperty(task, "outputJar");
                    if (outputJar instanceof Provider) {
                        outputJar = ((Provider<?>) outputJar).get();
                    }
                    sourcesPath = ((File) ReflectUtil.callMethod(outputJar, "getAsFile")).getAbsolutePath();
                } else {
                    Object sourcesOutputJar = ReflectUtil.getProperty(task, "sourcesOutputJar");
                    if (sourcesOutputJar instanceof Provider) {
                        sourcesOutputJar = ((Provider<?>) sourcesOutputJar).get();
                    }
                    sourcesPath = ((File) ReflectUtil.callMethod(sourcesOutputJar, "getAsFile")).getAbsolutePath();
                }
                String name = (String) ReflectUtil.getProperty(option, "name");
                String taskName = (String) ReflectUtil.getProperty(task, "name");
                result.add(new FabricLoomModelImpl.DecompilerModelImpl(name, taskName, sourcesPath));
            }
        }
        return result;
    }

    private FabricLoomModel buildLegacy(Project project, Object loomExtension) throws Exception {
        Object mappingsProvider = ReflectUtil.getProperty(loomExtension, "mappingsProvider");
        Object tinyMappingsObj = ReflectUtil.getProperty(mappingsProvider, "tinyMappings");
        File tinyMappings = null;
        if (tinyMappingsObj instanceof File) {
            tinyMappings = ((File) tinyMappingsObj).getAbsoluteFile();
        } else if (tinyMappingsObj != null) {
            tinyMappings = project.file(tinyMappingsObj).getAbsoluteFile();
        }

        Object decompilerOptions = ReflectUtil.getProperty(loomExtension, "decompilerOptions");
        List<FabricLoomModel.DecompilerModel> decompilers = new ArrayList<>();
        if (decompilerOptions instanceof Iterable) {
            for (Object option : (Iterable<?>) decompilerOptions) {
                String optionName = (String) ReflectUtil.getProperty(option, "name");
                String capitalized = Character.toUpperCase(optionName.charAt(0)) + optionName.substring(1);
                Object task = project.getTasks().getByName("genSourcesWith" + capitalized);

                Object runtimeJar = ReflectUtil.getProperty(task, "runtimeJar");
                if (runtimeJar instanceof Provider) {
                    runtimeJar = ((Provider<?>) runtimeJar).get();
                }
                File runtimeJarFile = (File) ReflectUtil.callMethod(runtimeJar, "getAsFile");
                String runtimeJarPath = runtimeJarFile.getAbsolutePath();
                String sourcesPath = runtimeJarPath.substring(0, runtimeJarPath.length() - 4) + "-sources.jar";

                String taskName = (String) ReflectUtil.getProperty(task, "name");
                decompilers.add(new FabricLoomModelImpl.DecompilerModelImpl(optionName, taskName, sourcesPath));
            }
        }

        Map<String, List<FabricLoomModel.DecompilerModel>> decompilersMap = new HashMap<>();
        decompilersMap.put("single", decompilers);
        return new FabricLoomModelImpl(tinyMappings, decompilersMap, false);
    }

    @Override
    public void reportErrorMessage(
        final @NotNull String modelName,
        final @NotNull Project project,
        final @NotNull ModelBuilderContext context,
        final @NotNull Exception exception
    ) {
        //noinspection UnstableApiUsage
        context.getMessageReporter().createMessage()
            .withGroup(this)
            .withKind(Message.Kind.ERROR)
            .withGroup("com.demonwav.mcdev")
            .withTitle("MinecraftDev: Loom import error")
            .withText("Unable to build MinecraftDev FabricLoom model for project " + project.getDisplayName())
            .withException(exception)
            .reportMessage(project);
    }
}
