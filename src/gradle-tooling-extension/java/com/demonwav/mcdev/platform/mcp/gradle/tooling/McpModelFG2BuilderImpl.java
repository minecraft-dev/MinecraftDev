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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class McpModelFG2BuilderImpl implements ModelBuilderService {

    @Override
    public boolean canBuild(String modelName) {
        return McpModelFG2.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        Object extension = project.getExtensions().findByName("minecraft");
        if (extension == null) {
            return null;
        }

        Task genSrgs = project.getTasks().findByName("genSrgs");
        if (genSrgs == null) {
            return null;
        }

        try {
            String version = (String) ReflectUtil.getProperty(extension, "version");
            String mappings = (String) ReflectUtil.getProperty(extension, "mappings");

            Set<File> files = genSrgs.getOutputs().getFiles().getFiles();
            Set<String> mappingFiles = new HashSet<>();
            for (File file : files) {
                mappingFiles.add(file.getAbsolutePath());
            }
            return new McpModelFG2Impl(version, mappings, mappingFiles);
        } catch (Exception e) {
            return null;
        }
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
