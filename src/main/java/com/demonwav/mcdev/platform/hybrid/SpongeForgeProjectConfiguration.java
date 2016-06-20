package com.demonwav.mcdev.platform.hybrid;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeTemplate;
import com.demonwav.mcdev.util.Util;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class SpongeForgeProjectConfiguration extends ForgeProjectConfiguration {

    public boolean generateDocumentation = false;
    public String spongeApiVersion;

    public SpongeForgeProjectConfiguration() {
        // We set our platform type to sponge because we want it to provide us the dependency. The GradleBuildSystem
        // will properly handle us as a combined project
        type = PlatformType.SPONGE;
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
                SpongeTemplate.applyMainClassTemplate(
                        project,
                        mainClassFile,
                        packageName,
                        className,
                        hasDependencies(),
                        generateDocumentation
                );

                writeMcmodInfo(project, buildSystem);

                PsiJavaFile mainClassPsi = (PsiJavaFile) PsiManager.getInstance(project).findFile(mainClassFile);
                if (mainClassPsi == null) {
                    return;
                }
                PsiClass psiClass = mainClassPsi.getClasses()[0];

                SpongeProjectConfiguration.writeMainSpongeClass(
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

                // Set the editor focus on the main class
                EditorHelper.openInEditor(mainClassPsi);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
