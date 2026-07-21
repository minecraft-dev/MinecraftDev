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

package com.demonwav.mcdev.platform.mcp.gradle.tooling.neomoddev;

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelNMD;
import com.demonwav.mcdev.platform.mcp.gradle.tooling.ReflectUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

public final class NeoModDevGradleModelBuilderImpl implements ModelBuilderService {

    @Override
    public boolean canBuild(String modelName) {
        return McpModelNMD.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        Object extension = project.getExtensions().findByName("neoForge");
        if (extension == null) {
            return null;
        }

        if (project.getPlugins().findPlugin("net.neoforged.moddev") == null) {
            return null;
        }

        String neoFormVersion = null;
        try {
            Object neoFormVersionProp = ReflectUtil.getProperty(extension, "neoFormVersion");
            if (neoFormVersionProp instanceof String) {
                neoFormVersion = (String) neoFormVersionProp;
            } else if (neoFormVersionProp instanceof Provider) {
                neoFormVersion = (String) ((Provider<?>) neoFormVersionProp).getOrNull();
            }
        } catch (Exception ignored) {
            // Happens when the NeoForm version is not set or throws
        }

        String neoforgeVersion = null;
        if (neoFormVersion == null) {
            try {
                Object neoforgeVersionProp = ReflectUtil.getProperty(extension, "version");
                if (neoforgeVersionProp instanceof String) {
                    neoforgeVersion = (String) neoforgeVersionProp;
                } else if (neoforgeVersionProp instanceof Provider) {
                    neoforgeVersion = (String) ((Provider<?>) neoforgeVersionProp).getOrNull();
                }
            } catch (Exception ignored) {
            }
            if (neoforgeVersion == null) {
                return null;
            }
        }

        List<File> accessTransformers = new ArrayList<>();
        try {
            Object accessTransformersRaw = ReflectUtil.getProperty(extension, "accessTransformers");
            if (accessTransformersRaw instanceof ListProperty) {
                ListProperty<?> listProperty = (ListProperty<?>) accessTransformersRaw;
                for (Object it : listProperty.get()) {
                    accessTransformers.add(project.file(it));
                }
            } else if (accessTransformersRaw instanceof FileCollection) {
                accessTransformers.addAll(((FileCollection) accessTransformersRaw).getFiles());
            } else if (accessTransformersRaw != null) {
                Object filesObj = ReflectUtil.getProperty(accessTransformersRaw, "files");
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

        File mappingsFile = null;
        try {
            Directory neoformDir = project.getLayout().getBuildDirectory().dir("neoForm").getOrNull();
            if (neoformDir != null) {
                final Path neoformDirPath = neoformDir.getAsFile().toPath();
                if (Files.exists(neoformDirPath)) {
                    try (Stream<Path> stream = Files.list(neoformDirPath)) {
                        mappingsFile = stream
                            .map(p -> p.resolve("config/joined.tsrg"))
                            .filter(Files::exists)
                            .findFirst()
                            .map(Path::toFile)
                            .orElse(null);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new NeoModDevGradleModelImpl(neoforgeVersion, neoFormVersion, mappingsFile, accessTransformers);
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
            .withTitle("MinecraftDev: NeoModDev import error")
            .withText("Unable to build MinecraftDev NeoModDev model for project " + project.getDisplayName())
            .withException(exception)
            .reportMessage(project);
    }
}
