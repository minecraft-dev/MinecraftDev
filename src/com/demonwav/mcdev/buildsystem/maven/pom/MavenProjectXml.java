package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTag;

public interface MavenProjectXml extends DomElement {

    @SubTag("modelVersion")
    ModelVersion getModelVersion();

    @SubTag("groupId")
    GroupId getGroupId();
    @SubTag("artifactId")
    ArtifactId getArtifactId();
    @SubTag("version")
    Version getVersion();
    @SubTag("packaging")
    Packaging getPackaging();

    @SubTag("name")
    Name getName();
    @SubTag("url")
    Url getUrl();

    @SubTag("repositories")
    Repositories getRepositories();
    @SubTag("dependencies")
    Dependencies getDependencies();
}
