/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.pluginyaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PermissionNode {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    public enum DefaultPermission { TRUE, FALSE, OP, NOT_OP }

    @NotNull private String name;
    @Nullable private String description;
    @NotNull private DefaultPermission defaultPermission = DefaultPermission.OP;
    @NotNull private Map<String, Boolean> children = new HashMap<>();

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
        return "PermissionNode{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", defaultPermission=" + defaultPermission +
                ", children=" + children +
                '}';
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PermissionNode that = (PermissionNode) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (defaultPermission != that.defaultPermission) {
            return false;
        }
        return children.equals(that.children);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + defaultPermission.hashCode();
        result = 31 * result + children.hashCode();
        return result;
    }
}
