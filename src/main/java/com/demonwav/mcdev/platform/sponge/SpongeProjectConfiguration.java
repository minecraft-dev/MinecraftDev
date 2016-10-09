/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.util.Util;

import com.google.common.base.Strings;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class SpongeProjectConfiguration extends ProjectConfiguration {

    public List<String> dependencies = new ArrayList<>();
    public boolean generateDocumentedListeners;
    public String spongeApiVersion;

    public SpongeProjectConfiguration() {
        type = PlatformType.SPONGE;
    }

    public boolean hasDependencies() {
        return listContainsAtLeastOne(dependencies);
    }

    public void setDependencies(String string) {
        this.dependencies.clear();
        Collections.addAll(this.dependencies, commaSplit(string));
    }

    @Override
    public void create(@NotNull Project project, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator) {
        Util.runWriteTask(() -> {
            try {
                indicator.setText("Writing main class");
                VirtualFile file = buildSystem.getSourceDirectories().get(0);
                String[] files = this.mainClass.split("\\.");
                String className = files[files.length - 1];
                String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                file = getMainClassDirectory(files, file);

                VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");
                SpongeTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className, hasDependencies(), generateDocumentedListeners);

                PsiJavaFile mainClassPsi = (PsiJavaFile) PsiManager.getInstance(project).findFile(mainClassFile);
                if (mainClassPsi == null) {
                    return;
                }
                PsiClass psiClass = mainClassPsi.getClasses()[0];

                writeMainSpongeClass(
                    project,
                    mainClassPsi,
                    psiClass,
                    buildSystem,
                    pluginName,
                    description,
                    website,
                    hasAuthors(),
                    authors,
                    hasDependencies(),
                    dependencies
                );

                EditorHelper.openInEditor(mainClassPsi);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Ugly hacks to avoid duplicate code
    public static void writeMainSpongeClass(
        @NotNull Project project,
        @NotNull PsiJavaFile mainClassPsi,
        @NotNull PsiClass psiClass,
        @NotNull BuildSystem buildSystem,
        @NotNull String pluginName,
        @NotNull String description,
        @NotNull String website,
        boolean hasAuthors,
        @NotNull List<String> authors,
        boolean hasDependencies,
        @NotNull List<String> dependencies
    ) {
        // I am absolutely sure this is not how this should be done. Raw string manipulation is messy and can
        // probably pretty easily break. However, I couldn't figure out the correct IntelliJ Psi way to do this,
        // and I didn't feel like spending stupid amounts of time trying to figure it out. This may get changed
        // in the future to a more correct method if someone can figure out how to properly do it.

        // Don't worry about whitespace between elements other than new-lines, the reformat operation will
        // handle indentation for us.

        String annotationString = "@Plugin(";
        annotationString += "\nid = \"" + buildSystem.getArtifactId().toLowerCase() + "\"";
        annotationString += ",\nname = \"" + pluginName + "\"";
        if (!(buildSystem instanceof GradleBuildSystem)) {
            // SpongeGradle will automatically set the Gradle version as plugin version
            annotationString += ",\nversion = \"" + buildSystem.getVersion() + "\"";
        }

        if (!Strings.isNullOrEmpty(description)) {
            annotationString += ",\ndescription = \"" + description + "\"";
        }

        if (!Strings.isNullOrEmpty(website)) {
            annotationString += ",\nurl = \"" + website + "\"";
        }

        if (hasAuthors) {
            annotationString += ",\nauthors = { ";
            Iterator<String> iterator = authors.iterator();
            while (iterator.hasNext()) {
                String author = iterator.next();
                annotationString += "\n\"" + author + "\"";
                if (iterator.hasNext()) {
                    annotationString += ", ";
                } else {
                    annotationString += " ";
                }
            }
            annotationString += "\n}";
        }

        if (hasDependencies) {
            annotationString += ",\ndependencies = { ";
            Iterator<String> iterator = dependencies.iterator();
            while (iterator.hasNext()) {
                String dependency = iterator.next();
                annotationString += "\n@Dependency(id = \"" + dependency + "\")";
                if (iterator.hasNext()) {
                    annotationString += ", ";
                } else {
                    annotationString += " ";
                }
            }
            annotationString += "\n}";
        }

        annotationString += "\n)";
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiAnnotation annotation = factory.createAnnotationFromText(annotationString, null);

        new WriteCommandAction.Simple(project, mainClassPsi) {
            @Override
            protected void run() throws Throwable {
                if (psiClass.getModifierList() != null) {
                    psiClass.getModifierList().addBefore(annotation, psiClass.getModifierList().getFirstChild());
                }
            }
        }.execute();
    }
}
