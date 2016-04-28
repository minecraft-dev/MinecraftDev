/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.util;

public class MavenSettings {
    public String groupId;
    public String artifactId;
    public String version;
    public String author;
    public String repoId;
    public String repoUrl;
    public String apiName;
    public String apiGroupId;
    public String apiArtifactId;
    public String apiVersion;

    public boolean hasAuthor() {
        return author != null && !author.trim().isEmpty();
    }
}
