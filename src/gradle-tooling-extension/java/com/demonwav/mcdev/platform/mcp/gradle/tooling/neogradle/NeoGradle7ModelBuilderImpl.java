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

package com.demonwav.mcdev.platform.mcp.gradle.tooling.neogradle;

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelNG7;
import com.demonwav.mcdev.platform.mcp.gradle.tooling.ReflectUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

public final class NeoGradle7ModelBuilderImpl implements ModelBuilderService {

    @Override
    public boolean canBuild(String modelName) {
        return McpModelNG7.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        Object extension = project.getExtensions().findByName("minecraft");
        if (extension == null) {
            return null;
        }

        if (project.getPlugins().findPlugin("net.neoforged.gradle.userdev") == null) {
            return null;
        }

        Object userDevRuntime = project.getExtensions().findByName("userDevRuntime");
        if (userDevRuntime == null) {
            return null;
        }

        String neoforgeVersion = null;
        try {
            Object runtimesObj = ReflectUtil.getProperty(userDevRuntime, "runtimes");
            if (runtimesObj instanceof Provider) {
                runtimesObj = ((Provider<?>) runtimesObj).get();
            }
            if (runtimesObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) runtimesObj).entrySet()) {
                    Object val = entry.getValue();
                    Object spec = ReflectUtil.getProperty(val, "specification");
                    Object forgeVersion = ReflectUtil.getProperty(spec, "forgeVersion");
                    if (forgeVersion != null) {
                        neoforgeVersion = forgeVersion.toString();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (neoforgeVersion == null) {
            return null;
        }

        Task neoFormMergeMappings = project.getTasks().findByName("neoFormMergeMappings");
        if (neoFormMergeMappings == null) {
            return null;
        }

        File mappingsFile = null;
        try {
            Object output = ReflectUtil.getProperty(neoFormMergeMappings, "output");
            if (output instanceof Provider) {
                output = ((Provider<?>) output).get();
            }
            if (output instanceof File) {
                mappingsFile = (File) output;
            } else if (output != null) {
                mappingsFile = (File) ReflectUtil.callMethod(output, "getAsFile");
            }
        } catch (Exception ignored) {
        }

        List<File> accessTransformers = new ArrayList<>();
        try {
            Object atObj = ReflectUtil.getProperty(extension, "accessTransformers");
            if (atObj instanceof FileCollection) {
                accessTransformers.addAll(((FileCollection) atObj).getFiles());
            } else if (atObj != null) {
                Object filesObj = ReflectUtil.getProperty(atObj, "files");
                if (filesObj instanceof Iterable) {
                    for (Object file : (Iterable<?>) filesObj) {
                        if (file instanceof File) {
                            accessTransformers.add((File) file);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new NeoGradle7ModelImpl(neoforgeVersion, mappingsFile, accessTransformers);
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
            .withTitle("MinecraftDev: NeoGradle import error")
            .withText("Unable to build MinecraftDev NeoGradle model for project " + project.getDisplayName())
            .withException(exception)
            .reportMessage(project);
    }
}
