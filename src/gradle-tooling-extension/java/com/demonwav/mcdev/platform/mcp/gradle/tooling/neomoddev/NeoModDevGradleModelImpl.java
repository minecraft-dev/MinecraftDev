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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;

final class NeoModDevGradleModelImpl implements McpModelNMD, Serializable {

    private final String neoForgeVersion;
    private final String neoFormVersion;
    private final File mappingsFile;
    private final List<File> accessTransformers;

    NeoModDevGradleModelImpl(
            String neoForgeVersion,
            String neoFormVersion,
            File mappingsFile,
            List<File> accessTransformers
    ) {
        this.neoForgeVersion = neoForgeVersion;
        this.neoFormVersion = neoFormVersion;
        this.mappingsFile = mappingsFile;
        this.accessTransformers = accessTransformers;
    }

    @Override
    @Nullable
    public String getNeoForgeVersion() {
        return neoForgeVersion;
    }

    @Override
    @Nullable
    public String getNeoFormVersion() {
        return neoFormVersion;
    }

    @Override
    public File getMappingsFile() {
        return mappingsFile;
    }

    @Override
    public List<File> getAccessTransformers() {
        return accessTransformers;
    }
}
