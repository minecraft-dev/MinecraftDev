package com.demonwav.mcdev.platform.bukkit.yaml;

import com.demonwav.mcdev.platform.bukkit.BukkitModule;
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder;

import com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class PluginConfig {

    /*
     *   TODO: Implement setters (not implemented yet because changes here must reflect back to the plugin.yml file)
     *   Any return list must be immutable (so someone can't make a change to the list and expect it to reflect
     *   back to the plugin.yml). List operations should be done with add and remove methods.
     */

    @NotNull private BukkitModule project;

    /**
     * The name of the plugin.
     */
    @NotNull private String name = "";
    /**
     * The fully qualified name of the main class for the plugin.
     */
    @NotNull private String main = "";
    /**
     * The version of the plugin.
     */
    @NotNull private String version = "";
    /**
     * The order in which the plugin should be loaded.
     *
     * @see LoadOrder
     */
    @NotNull private LoadOrder load = LoadOrder.POSTWORLD;
    /**
     * A list of plugins the plugin should load before.
     *
     * <p>The plugin will be loaded <b>before</b> plugins specified in this list.</p>
     */
    @NotNull
    @SuppressWarnings("SpellCheckingInspection")
    private List<String> loadbefore = new ArrayList<>();
    /**
     * A list of plugins that the plugin has a hard dependency on.
     *
     * <p>The plugin will be loaded <b>after</b> plugins specified in this list.</p>
     */
    @NotNull private List<String> depend = new ArrayList<>();
    /**
     * A list of plugins that the plugin has a soft dependency on.
     *
     * <p>The plugin will be loaded <b>after</b> plugins specified in this list.</p>
     */
    @NotNull
    @SuppressWarnings("SpellCheckingInspection")
    private List<String> softdepend = new ArrayList<>();
    /**
     * The name of a single author.
     *
     * @see #authors
     */
    @Nullable private String author;
    /**
     * A list of author names.
     *
     * @see #author
     */
    @NotNull private List<String> authors = new ArrayList<>();
    /**
     * A human-friendly description of the functionality the plugin provides.
     */
    @Nullable private String description;
    /**
     * The plugin's, or plugin's author's, website.
     */
    @Nullable private String website;
    /**
     * The token to prefix plugin-specific logging messages with.
     */
    @Nullable private String prefix;
    @NotNull private CommandList commands = new CommandList();
    @NotNull private PermissionList permissions = new PermissionList();
    private boolean database;

    public PluginConfig(@NotNull BukkitModule project) {
        this.project = project;
    }

    @NotNull
    public BukkitModule getProject() {
        return project;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {

    }

    @NotNull
    public String getMain() {
        return main;
    }

    public void setMain(@NotNull String main) {

    }

    @NotNull
    public String getVersion() {
        return version;
    }

    public void setVersion(@NotNull String version) {

    }

    @NotNull
    public LoadOrder getLoad() {
        return load;
    }

    public void setLoad(@NotNull LoadOrder load) {

    }

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

    @Nullable
    public String getAuthor() {
        return author;
    }

    public void setAuthor(@Nullable String author) {

    }

    @NotNull
    public List<String> getAuthors() {
        return Collections.unmodifiableList(this.authors);
    }

    public boolean addAuthor(@NotNull String... authors) {
        return false;
    }

    public boolean removeAuthor(@NotNull String... authors) {
        return false;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {

    }

    @Nullable
    public String getWebsite() {
        return website;
    }

    public void setWebsite(@Nullable String website) {

    }

    @Nullable
    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(@Nullable String prefix) {

    }

    public boolean isDatabase() {
        return database;
    }

    public void setDatabase(boolean database) {

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", this.name)
                .add("main", this.main)
                .add("version", this.version)
                .add("load", this.load)
                .add("loadbefore", this.loadbefore)
                .add("depend", this.depend)
                .add("softdepend", this.softdepend)
                .add("author", this.author)
                .add("authors", this.authors)
                .add("description", this.description)
                .add("website", this.website)
                .add("prefix", this.prefix)
                .add("commands", this.commands)
                .add("permissions", this.permissions)
                .add("database", this.database)
                .toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        PluginConfig that = (PluginConfig) other;
        return Objects.equal(this.name, that.name)
                && Objects.equal(this.main, that.main)
                && Objects.equal(this.version, that.version)
                && this.load == that.load
                && Objects.equal(this.loadbefore, that.loadbefore)
                && Objects.equal(this.depend, that.depend)
                && Objects.equal(this.softdepend, that.softdepend)
                && Objects.equal(this.author, that.author)
                && Objects.equal(this.authors, that.authors)
                && Objects.equal(this.description, that.description)
                && Objects.equal(this.website, that.website)
                && Objects.equal(this.prefix, that.prefix)
                && Objects.equal(this.commands, that.commands)
                && Objects.equal(this.permissions, that.permissions)
                && this.database == that.database;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name, this.main, this.version, this.load, this.loadbefore, this.depend,
                this.softdepend, this.author, this.authors, this.description, this.website, this.prefix,
                this.commands, this.permissions, this.database);
    }
}
