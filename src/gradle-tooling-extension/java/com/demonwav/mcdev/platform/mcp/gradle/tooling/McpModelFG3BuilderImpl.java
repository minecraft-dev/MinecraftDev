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

package com.demonwav.mcdev.platform.mcp.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class McpModelFG3BuilderImpl implements ModelBuilderService {

    @Override
    public boolean canBuild(String modelName) {
        return McpModelFG3.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        Object extension = project.getExtensions().findByName("minecraft");
        if (extension == null) {
            return null;
        }

        if (project.getPlugins().findPlugin("net.minecraftforge.gradle") == null) {
            return null;
        }

        List<String> minecraftDepVersions = new ArrayList<>();
        Configuration minecraftConfig = project.getConfigurations().findByName("minecraft");
        if (minecraftConfig != null) {
            for (Dependency dep : minecraftConfig.getDependencies()) {
                minecraftDepVersions.add(dep.getVersion());
            }
        }

        Task task = project.getTasks().findByName("createMcpToSrg");
        if (task == null) {
            task = project.getTasks().findByName("createMcp2Srg");
        }
        if (task == null) {
            return null;
        }

        File taskOutput = task.getOutputs().getFiles().getSingleFile();

        String mappings = null;
        try {
            Object mappingsObj = ReflectUtil.getProperty(extension, "mappings");
            if (mappingsObj instanceof Provider) {
                mappingsObj = ((Provider<?>) mappingsObj).get();
            }
            if (mappingsObj != null) {
                mappings = mappingsObj.toString();
            }
        } catch (Exception ignored) {
        }

        List<File> accessTransformers = new ArrayList<>();
        try {
            Object atObj = ReflectUtil.getProperty(extension, "accessTransformers");
            if (atObj instanceof FileCollection) {
                accessTransformers.addAll(((FileCollection) atObj).getFiles());
            } else if (atObj instanceof Iterable) {
                for (Object item : (Iterable<?>) atObj) {
                    if (item instanceof File) {
                        accessTransformers.add((File) item);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new McpModelFG3Impl(minecraftDepVersions, mappings, taskOutput, task.getName(), accessTransformers);
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
            .withTitle("MinecraftDev: ForgeGradle import error")
            .withText("Unable to build MinecraftDev ForgeGradle model for project " + project.getDisplayName())
            .withException(exception)
            .reportMessage(project);
    }
}
