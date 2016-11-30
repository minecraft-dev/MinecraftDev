/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class BuildDependency {

    private String artifactId;
    private String groupId;
    private String version;
    private String scope;

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public BuildDependency() {}

    public BuildDependency(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("artifactId", artifactId)
                          .add("groupId", groupId)
                          .add("version", version)
                          .add("scope", scope)
                          .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildDependency that = (BuildDependency) o;
        return Objects.equal(artifactId, that.artifactId) &&
            Objects.equal(groupId, that.groupId) &&
            Objects.equal(version, that.version) &&
            Objects.equal(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(artifactId, groupId, version, scope);
    }
}
