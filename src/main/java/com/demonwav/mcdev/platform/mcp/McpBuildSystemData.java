/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public class McpBuildSystemData {

    private String minecraftVersion;
    private String mcpVersion;

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public String getMcpVersion() {
        return mcpVersion;
    }

    public void setMcpVersion(String mcpVersion) {
        this.mcpVersion = mcpVersion;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("minecraftVersion", minecraftVersion)
                          .add("mcpVersion", mcpVersion)
                          .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final McpBuildSystemData that = (McpBuildSystemData) o;
        return Objects.equals(minecraftVersion, that.minecraftVersion) && Objects.equals(mcpVersion, that.mcpVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minecraftVersion, mcpVersion);
    }
}
