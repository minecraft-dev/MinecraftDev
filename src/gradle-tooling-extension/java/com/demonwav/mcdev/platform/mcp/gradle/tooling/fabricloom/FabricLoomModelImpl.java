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

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class FabricLoomModelImpl implements FabricLoomModel, Serializable {

    private final String minecraftVersion;
    private final File tinyMappings;
    private final Map<String, List<DecompilerModel>> decompilers;
    private final boolean splitMinecraftJar;

    public FabricLoomModelImpl(
            String minecraftVersion,
            @Nullable File tinyMappings,
            Map<String, List<DecompilerModel>> decompilers,
            boolean splitMinecraftJar
    ) {
        this.minecraftVersion = minecraftVersion;
        this.tinyMappings = tinyMappings;
        this.decompilers = decompilers;
        this.splitMinecraftJar = splitMinecraftJar;
    }

    public FabricLoomModelImpl(
            @Nullable File tinyMappings,
            Map<String, List<DecompilerModel>> decompilers,
            boolean splitMinecraftJar
    ) {
        this(null, tinyMappings, decompilers, splitMinecraftJar);
    }

    @Override
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    @Nullable
    public File getTinyMappings() {
        return tinyMappings;
    }

    @Override
    public Map<String, List<DecompilerModel>> getDecompilers() {
        return decompilers;
    }

    @Override
    public boolean getSplitMinecraftJar() {
        return splitMinecraftJar;
    }

    public static final class DecompilerModelImpl implements DecompilerModel, Serializable {
        private final String name;
        private final String taskName;
        private final String sourcesPath;

        public DecompilerModelImpl(String name, String taskName, String sourcesPath) {
            this.name = name;
            this.taskName = taskName;
            this.sourcesPath = sourcesPath;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getTaskName() {
            return taskName;
        }

        @Override
        public String getSourcesPath() {
            return sourcesPath;
        }
    }
}
