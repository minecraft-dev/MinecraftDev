package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;

public interface Repository extends DomElement {

    Id getId();
    Url getUrl();
}
