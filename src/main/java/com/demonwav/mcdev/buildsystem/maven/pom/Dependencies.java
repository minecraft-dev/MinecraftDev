/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTagList;

import java.util.List;

public interface Dependencies extends DomElement {

    @SubTagList("dependency")
    List<Dependency> getDependencies();

    Dependency addDependency();
}
