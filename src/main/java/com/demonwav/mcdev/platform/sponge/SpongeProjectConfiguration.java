package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.google.common.base.Strings;
import com.intellij.codeInspection.ex.EntryPointsManager;
import com.intellij.codeInspection.ex.EntryPointsManagerBase;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.JDOMExternalizableStringList;
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

public class SpongeProjectConfiguration extends ProjectConfiguration {

    public List<String> dependencies = new ArrayList<>();
    public boolean generateDocumentedListeners;

    public boolean hasDependencies() {
        return listContainsAtLeastOne(dependencies);
    }

    public void setDependencies(String string) {
        this.dependencies.clear();
        Collections.addAll(this.dependencies, commaSplit(string));
    }

    @Override
    public void create(@NotNull Module module, @NotNull PlatformType type, @NotNull BuildSystem buildSystem) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile file = buildSystem.getSourceDirectories().get(0);
                String[] files = this.mainClass.split("\\.");
                String className = files[files.length - 1];
                String packageName = this.mainClass.substring(0, this.mainClass.length() - className.length() - 1);
                for (int i = 0, len = files.length - 1; i < len; i++) {
                    String s = files[i];
                    file = file.createChildDirectory(this, s);
                }

                VirtualFile mainClassFile = file.findOrCreateChildData(this, className + ".java");
                SpongeTemplate.applyMainClassTemplate(module, mainClassFile, packageName, className, this);

                PsiJavaFile mainClassPsi = (PsiJavaFile) PsiManager.getInstance(module.getProject()).findFile(mainClassFile);
                assert mainClassPsi != null;
                PsiClass psiClass = mainClassPsi.getClasses()[0];

                PsiElementFactory factory = JavaPsiFacade.getElementFactory(module.getProject());

                // I am absolutely sure this is not how this should be done. Raw string manipulation is messy and can
                // probably pretty easily break. However, I couldn't figure out the correct IntelliJ Psi way to do this,
                // and I didn't feel like spending stupid amounts of time trying to figure it out. This may get changed
                // in the future to a more correct method if someone can figure out how to properly do it.

                // Don't worry about whitespace between elements other than new-lines, the reformat operation will
                // handle indentation for us.

                String annotationString = "@Plugin(";
                annotationString += "\nid = \"" + buildSystem.getGroupId() + "." + buildSystem.getArtifactId() + "\"";
                annotationString += ",\nname = \"" + pluginName + "\"";
                annotationString += ",\nversion = \"" + buildSystem.getVersion() + "\"";

                if (!Strings.isNullOrEmpty(description)) {
                    annotationString += ",\ndescription = \"" + description + "\"";
                }

                if (!Strings.isNullOrEmpty(website)) {
                    annotationString += ",\nurl = \"" + website + "\"";
                }

                if (hasAuthors()) {
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

                if (hasDependencies()) {
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
                PsiAnnotation annotation = factory.createAnnotationFromText(annotationString, null);

                new WriteCommandAction.Simple(module.getProject(), mainClassPsi) {
                    @Override
                    protected void run() throws Throwable {
                        psiClass.getModifierList().addBefore(annotation, psiClass.getModifierList().getFirstChild());
                    }
                }.execute();

                performCreationSettingSetup(module, type);
                EditorHelper.openInEditor(mainClassPsi);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
