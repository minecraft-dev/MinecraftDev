/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.pluginyaml;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
@EqualsAndHashCode
public class CommandNode {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    @Getter @NotNull private String name;
    @Getter @Nullable private String description;
    @NotNull private List<String> aliases = new ArrayList<>();
    @Getter @Nullable private String permission;
    @Getter @Nullable private String permissionMessage;
    @Getter @Nullable private String usage;

    // Constructors
    public CommandNode(@NotNull String name) {
        this.name = name;
    }

    public CommandNode(@NotNull String name, @NotNull String... aliases) {
        this.name = name;
        Collections.addAll(this.aliases, aliases);
    }

    // Name
    public void setName(@NotNull String name) {

    }

    // Description
    public void setDescription(@Nullable String description) {

    }

    // Alias
    @NotNull
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    public boolean addAlias(@NotNull String alias) {
        return false;
    }

    public boolean removeAlias(@NotNull String alias) {
        return false;
    }

    public boolean containsAlias(@NotNull String alias) {
        return aliases.contains(alias);
    }

    // Permission
    public void setPermission(@Nullable String permission) {

    }

    // Permission
    public void setPermissionMessage(@Nullable String permissionMessage) {

    }

    // Usage
    public void setUsage(@Nullable String usage) {

    }
}
