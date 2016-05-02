package com.demonwav.mcdev.buildsystem.maven.pom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTagList;

import java.util.List;

public interface Repositories extends DomElement {

    @SubTagList("repository")
    List<Repository> getRepositories();

    Repository addRepository();
}
