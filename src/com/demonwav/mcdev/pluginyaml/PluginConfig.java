/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.pluginyaml;

import com.demonwav.mcdev.BukkitProject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginConfig {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    public enum Load { STARTUP, POSTWORLD }

    @NotNull private BukkitProject project;

    // NotNull values
    @NotNull private String name = "";
    @NotNull private String version = "";
    @NotNull private Load load = Load.POSTWORLD;
    @NotNull private List<String> authors = new ArrayList<>();
    @NotNull private String main = "";
    @NotNull private CommandList commands = new CommandList();
    @NotNull private PermissionList permissions = new PermissionList();

    // Nullable values
    @Nullable private String description;
    @Nullable private String author;
    @Nullable private String website;
    @Nullable private String prefix;

    // Primitive value
    private boolean database;

    // NotNull values with custom getters
    @NotNull private List<String> depend = new ArrayList<>();
    @NotNull private List<String> softdepend = new ArrayList<>();
    @NotNull private List<String> loadbefore = new ArrayList<>();

    public PluginConfig(@NotNull BukkitProject project) {
        this.project = project;
    }

    @NotNull
    public BukkitProject getProject() {
        return project;
    }

    // Name
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {

    }

    // Version
    @NotNull
    public String getVersion() {
        return version;
    }

    public void setVersion(@NotNull String version) {

    }

    // Description
    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {

    }

    // Load
    @NotNull
    public Load getLoad() {
        return load;
    }

    public void setLoad(@NotNull Load load) {

    }

    // Author
    @Nullable
    public String getAuthor() {
        return author;
    }

    public void setAuthor(@Nullable String author) {

    }

    // Website
    @Nullable
    public String getWebsite() {
        return website;
    }

    public void setWebsite(@Nullable String website) {

    }

    // Main
    @NotNull
    public String getMain() {
        return main;
    }

    public void setMain(@NotNull String main) {

    }

    // Database
    public boolean isDatabase() {
        return database;
    }

    public void setDatabase(boolean database) {

    }

    // Depend
    @NotNull
    public List<String> getDepend() {
        return Collections.unmodifiableList(depend);
    }

    public boolean addDepend(@NotNull String... depend) {
        return false;
    }

    public boolean removeDepend(@NotNull String... depend) {
        return false;
    }

    // Prefix
    public void setPrefix(@Nullable String prefix) {

    }

    // Soft Depend
    @NotNull
    public List<String> getSoftdepend() {
        return Collections.unmodifiableList(softdepend);
    }

    public boolean addSoftdepend(@NotNull String... softdepend) {
        return false;
    }

    public boolean removeSoftdepend(@NotNull String... softdepend) {
        return false;
    }

    // Load Before
    @NotNull
    public List<String> getLoadbefore() {
        return Collections.unmodifiableList(loadbefore);
    }

    public boolean addLoadbefore(@NotNull String... loadbefore) {
        return false;
    }

    public boolean removeLoadbefore(@NotNull String... loadbefore) {
        return false;
    }

    @Override
    public String toString() {
        return "PluginConfig{" +
                "loadbefore=" + loadbefore +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", load=" + load +
                ", authors=" + authors +
                ", main='" + main + '\'' +
                ", commands=" + commands +
                ", permissions=" + permissions +
                ", description='" + description + '\'' +
                ", author='" + author + '\'' +
                ", website='" + website + '\'' +
                ", prefix='" + prefix + '\'' +
                ", database=" + database +
                ", depend=" + depend +
                ", softdepend=" + softdepend +
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

        PluginConfig that = (PluginConfig) o;

        if (database != that.database) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        if (!version.equals(that.version)) {
            return false;
        }
        if (load != that.load) return false;
        if (!authors.equals(that.authors)) {
            return false;
        }
        if (!main.equals(that.main)) {
            return false;
        }
        if (!commands.equals(that.commands)) {
            return false;
        }
        if (!permissions.equals(that.permissions)) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (author != null ? !author.equals(that.author) : that.author != null) {
            return false;
        }
        if (website != null ? !website.equals(that.website) : that.website != null) {
            return false;
        }
        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) {
            return false;
        }
        if (!depend.equals(that.depend)) {
            return false;
        }
        if (!softdepend.equals(that.softdepend)) {
            return false;
        }
        return loadbefore.equals(that.loadbefore);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + load.hashCode();
        result = 31 * result + authors.hashCode();
        result = 31 * result + main.hashCode();
        result = 31 * result + commands.hashCode();
        result = 31 * result + permissions.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (website != null ? website.hashCode() : 0);
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (database ? 1 : 0);
        result = 31 * result + depend.hashCode();
        result = 31 * result + softdepend.hashCode();
        result = 31 * result + loadbefore.hashCode();
        return result;
    }
}
