/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.pluginyaml;

import com.demonwav.bukkitplugin.BukkitProject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString(exclude = "project")
@EqualsAndHashCode
public class PluginConfig {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    public enum Load {STARTUP, POSTWORLD}

    @Getter @NotNull private BukkitProject project;

    // NotNull values with automatic getters
    @Getter @NotNull private String name = "";
    @Getter @NotNull private String version = "";
    @Getter @NotNull private Load load = Load.POSTWORLD;
    @Getter @NotNull private List<String> authors = new ArrayList<>();
    @Getter @NotNull private String main = "";
    @Getter @NotNull private CommandList commands = new CommandList();
    @Getter @NotNull private PermissionList permissions = new PermissionList();

    // Nullable values with automatic getters
    @Getter @Nullable private String description;
    @Getter @Nullable private String author;
    @Getter @Nullable private String website;
    @Getter @Nullable private String prefix;

    // Primitive value with automatic getter
    @Getter private boolean database;

    // NotNull values with custom getters
    @NotNull private List<String> depend = new ArrayList<>();
    @NotNull private List<String> softdpend = new ArrayList<>();
    @NotNull private List<String> loadbefore = new ArrayList<>();

    public PluginConfig(@NotNull BukkitProject project) {
        this.project = project;
    }

    // Name
    public void setName(@NotNull String name) {

    }

    // Version
    public void setVersion(@NotNull String version) {

    }

    // Description
    public void setDescription(@Nullable String description) {

    }

    // Load
    public void setLoad(@NotNull Load load) {

    }

    // Author
    public void setAuthor(@Nullable String author) {

    }

    // Website
    public void setWebsite(@Nullable String website) {

    }

    // Main
    public void setMain(@NotNull String main) {

    }

    // Database
    public void setDatabase(boolean database) {

    }

    // Depend
    @NotNull
    public List<String> getDepend() {
        return Collections.unmodifiableList(depend);
    }

    public boolean addDepend(@NotNull String depend) {
        return false;
    }

    public boolean removeDepend(@NotNull String depend) {
        return false;
    }

    // Prefix
    public void setPrefix(@Nullable String prefix) {

    }

    // Soft Depend
    @NotNull
    public List<String> getSoftDepend() {
        return Collections.unmodifiableList(softdpend);
    }

    public boolean addSoftDepend(@NotNull String softdpend) {
        return false;
    }

    public boolean removeSoftDepend(@NotNull String softDepend) {
        return false;
    }

    // Load Before
    @NotNull
    public List<String> getLoadbefore() {
        return Collections.unmodifiableList(loadbefore);
    }

    public boolean addLoadBefore(@NotNull String loadbefore) {
        return false;
    }

    public boolean removeLoadBefore(@NotNull String loadBefore) {
        return false;
    }
}
