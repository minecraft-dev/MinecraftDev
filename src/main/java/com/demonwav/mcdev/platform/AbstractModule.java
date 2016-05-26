package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import javax.swing.Icon;

public abstract class AbstractModule {
    protected Module module;
    protected BuildSystem buildSystem;

    public Module getModule() {
        return module;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public abstract AbstractModuleType getModuleType();
    public abstract PlatformType getType();
    public abstract Icon getIcon();

    /**
     * By default, this method is provided in the case that a specific platform has no
     * listener handling whatsoever, or simply accepts event listeners with random
     * classes. This is rather open ended. Primarily this should (platform dependent)
     * evaluate to the type (or multiple types) to determine whether the event listener
     * is not going to throw an error at runtime.
     *
     * @param eventClass The PsiClass of the event listener argument
     * @param method The method of the event listener
     * @return True if the class is valid or ignored. Returning false may highlight the
     *     method as an error and prevent compiling.
     */
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return true;
    }

    public String writeErrorMessageForEventParameter(PsiClass eventClass) {
        return "Parameter does not extend the proper Event Class!";
    }
}
