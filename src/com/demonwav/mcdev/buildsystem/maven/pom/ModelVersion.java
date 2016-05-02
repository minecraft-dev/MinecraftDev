package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;

public interface ModelVersion extends DomElement {

    String getValue();
    void setValue(String s);
}
