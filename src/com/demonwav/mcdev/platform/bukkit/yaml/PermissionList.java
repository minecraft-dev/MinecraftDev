package com.demonwav.mcdev.platform.bukkit.yaml;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionList {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    @NotNull
    private List<PermissionNode> permissions = new ArrayList<>();

    /**
     * Searches for and returns any permissions which go by the provided name. Returns an empty list if none are
     * found. The search is not case sensitive.
     *
     * @param permission Permission to search for
     * @return Permissinos whose names match the given name, or an empty list if none are found
     */
    @NotNull
    public List<PermissionNode> getPermission(@NotNull String permission) {
        return permissions.stream().filter(node -> node.getName().equalsIgnoreCase(permission)).collect(Collectors.toList());
    }

    @NotNull
    public List<PermissionNode> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }

    public boolean addPermission(@NotNull PermissionNode node) {
        return false;
    }

    public boolean removePermission(@NotNull String name) {
        return false;
    }

    public boolean containsPermission(@NotNull String name) {
        return permissions.stream().anyMatch(node -> node.getName().equalsIgnoreCase(name));
    }

    @Override
    public String toString() {
        return "PermissionList{" +
                "permissions=" + permissions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PermissionList that = (PermissionList) o;

        return permissions.equals(that.permissions);

    }

    @Override
    public int hashCode() {
        return permissions.hashCode();
    }
}
