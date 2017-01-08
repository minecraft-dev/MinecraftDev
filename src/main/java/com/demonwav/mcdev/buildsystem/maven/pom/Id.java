/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;

public interface Id extends DomElement {

    String getValue();
    void setValue(String s);
}
