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

import java.io.File;
import java.util.List;

final class McpModelFG3Impl implements McpModelFG3, java.io.Serializable {

    private final List<String> minecraftDepVersions;
    private final String mcpVersion;
    private final File taskOutputLocation;
    private final String taskName;
    private final List<File> accessTransformers;

    McpModelFG3Impl(
            List<String> minecraftDepVersions,
            String mcpVersion,
            File taskOutputLocation,
            String taskName,
            List<File> accessTransformers
    ) {
        this.minecraftDepVersions = minecraftDepVersions;
        this.mcpVersion = mcpVersion;
        this.taskOutputLocation = taskOutputLocation;
        this.taskName = taskName;
        this.accessTransformers = accessTransformers;
    }

    @Override
    public List<String> getMinecraftDepVersions() {
        return minecraftDepVersions;
    }

    @Override
    public String getMcpVersion() {
        return mcpVersion;
    }

    @Override
    public File getTaskOutputLocation() {
        return taskOutputLocation;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public List<File> getAccessTransformers() {
        return accessTransformers;
    }
}
