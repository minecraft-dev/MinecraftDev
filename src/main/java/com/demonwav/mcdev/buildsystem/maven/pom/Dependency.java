/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTag;

public interface Dependency extends DomElement {

    @SubTag("groupId")
    GroupId getGroupId();
    @SubTag("artifactId")
    ArtifactId getArtifactId();
    @SubTag("version")
    Version getVersion();
    @SubTag("scope")
    Scope getScope();
}
