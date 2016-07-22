package com.demonwav.mcdev.platform.bukkit.yaml;

import com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class CommandNode {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    @NotNull private String name;
    @Nullable private String description;
    @NotNull private List<String> aliases = new ArrayList<>();
    @Nullable private String permission;
    @Nullable private String permissionMessage;
    @Nullable private String usage;

    // Constructors
    public CommandNode(@NotNull String name) {
        this.name = name;
    }

    public CommandNode(@NotNull String name, @NotNull String... aliases) {
        this.name = name;
        Collections.addAll(this.aliases, aliases);
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
    @Nullable
    public String getPermission() {
        return permission;
    }

    public void setPermission(@Nullable String permission) {

    }

    // Permission Message
    @Nullable
    public String getPermissionMessage() {
        return permissionMessage;
    }

    public void setPermissionMessage(@Nullable String permissionMessage) {

    }

    // Usage
    @Nullable
    public String getUsage() {
        return usage;
    }

    public void setUsage(@Nullable String usage) {

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("description", description)
            .add("aliases", aliases)
            .add("permission", permission)
            .add("permissionMessage", permissionMessage)
            .add("usage", usage)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandNode that = (CommandNode) o;
        return Objects.equal(name, that.name) &&
            Objects.equal(description, that.description) &&
            Objects.equal(aliases, that.aliases) &&
            Objects.equal(permission, that.permission) &&
            Objects.equal(permissionMessage, that.permissionMessage) &&
            Objects.equal(usage, that.usage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, description, aliases, permission, permissionMessage, usage);
    }
}
