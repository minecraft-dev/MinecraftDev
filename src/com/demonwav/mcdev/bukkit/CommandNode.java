package com.demonwav.mcdev.bukkit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return "CommandNode{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", aliases=" + aliases +
                ", permission='" + permission + '\'' +
                ", permissionMessage='" + permissionMessage + '\'' +
                ", usage='" + usage + '\'' +
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

        CommandNode that = (CommandNode) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (!aliases.equals(that.aliases)) {
            return false;
        }
        if (permission != null ? !permission.equals(that.permission) : that.permission != null) {
            return false;
        }
        if (permissionMessage != null ? !permissionMessage.equals(that.permissionMessage) : that.permissionMessage != null) {
            return false;
        }
        return usage != null ? usage.equals(that.usage) : that.usage == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + aliases.hashCode();
        result = 31 * result + (permission != null ? permission.hashCode() : 0);
        result = 31 * result + (permissionMessage != null ? permissionMessage.hashCode() : 0);
        result = 31 * result + (usage != null ? usage.hashCode() : 0);
        return result;
    }
}
