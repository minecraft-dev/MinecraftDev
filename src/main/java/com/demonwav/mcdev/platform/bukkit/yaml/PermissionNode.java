/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.yaml;

import com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PermissionNode {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    public enum DefaultPermission { TRUE, FALSE, OP, NOT_OP }

    @NotNull private final String name;
    @Nullable private String description;
    @NotNull private final DefaultPermission defaultPermission = DefaultPermission.OP;
    @NotNull private final Map<String, Boolean> children = new HashMap<>();

    public PermissionNode(@NotNull String name) {
        this.name = name;
    }

    // Name
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {

    }

    // Description
    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {

    }

    // Default Permission
    @NotNull
    public DefaultPermission getDefaultPermission() {
        return defaultPermission;
    }

    public void setDefaultPermission(@NotNull DefaultPermission defaultPermission) {

    }

    // Children
    public Map<String, Boolean> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public boolean addChild(@NotNull String name, boolean inheritPerms) {
        return false;
    }

    public boolean removeChild(@NotNull String name) {
        return false;
    }

    public boolean containsChild(@NotNull String name) {
        return children.containsKey(name);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("description", description)
            .add("defaultPermission", defaultPermission)
            .add("children", children)
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
        PermissionNode that = (PermissionNode) o;
        return Objects.equal(name, that.name) &&
            Objects.equal(description, that.description) &&
            defaultPermission == that.defaultPermission &&
            Objects.equal(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, description, defaultPermission, children);
    }
}
