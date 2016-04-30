/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.pluginyaml;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class PermissionNode {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    public enum DefaultPermission { TRUE, FALSE, OP, NOT_OP }

    @Getter @NotNull private String name;
    @Getter @Nullable private String description;
    @Getter @NotNull private DefaultPermission defaultPermission = DefaultPermission.OP;
    @NotNull private Map<String, Boolean> children = new HashMap<>();

    public PermissionNode(@NotNull String name) {
        this.name = name;
    }

    // Name
    public void setName(@NotNull String name) {

    }

    // Description
    public void setDescription(@Nullable String description) {

    }

    // Default Permission
    public void setDefaultPermission(@NotNull DefaultPermission defaultPermission) {

    }

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
}
