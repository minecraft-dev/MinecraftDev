/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.BukkitPlugin.project;

import com.demonwav.BukkitPlugin.BukkitProject;
import com.demonwav.BukkitPlugin.icons.BukkitProjectsIcons;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import java.io.File;

public class ProjectBuilder extends JavaModuleBuilder {

    private BukkitProject project;
    private MavenProjectCreator creator = new MavenProjectCreator();

    public ProjectBuilder(BukkitProject project) {
        this.project = project;
    }

    @Override
    public String getPresentableName() {
        return "Bukkit Plugin";
    }

    @Override
    public Icon getNodeIcon() {
            return BukkitProjectsIcons.BukkitProject;
    }

    @Override
    public String getGroupName() {
        return "Bukkit Plugin";
    }

    @Override
    public int getWeight() {
        return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT - 1;
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdk) {
        return sdk == JavaSdk.getInstance();
    }

    @Override
    public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        final Project project = modifiableRootModel.getProject();
        final VirtualFile root = createAndGetRoot();
        modifiableRootModel.addContentEntry(root);

        if (getModuleJdk() != null)
            modifiableRootModel.setSdk(getModuleJdk());


        creator.setRoot(root);
        creator.setProject(project);
        this.project.setProjectType(creator.getType());

        DumbAwareRunnable r = creator::create;

        if (project.isDisposed())
            return;

        if (ApplicationManager.getApplication().isUnitTestMode()
            || ApplicationManager.getApplication().isHeadlessEnvironment()) {
            r.run();
            return;
        }


        if (!project.isInitialized()) {
            StartupManager.getInstance(project).registerPostStartupActivity(r);
            return;
        }

        if (DumbService.isDumbAware(r)) {
            r.run();
        } else {
            DumbService.getInstance(project).runWhenSmart(r);
        }
    }

    private VirtualFile createAndGetRoot() {
        String temp = getContentEntryPath();

        assert temp != null;

        String path = FileUtil.toSystemIndependentName(temp);
        //noinspection ResultOfMethodCallIgnored
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    @Override
    public BukkitProjectModule getModuleType() {
        return BukkitProjectModule.getInstance();
    }

    @Override
    public String getParentGroup() {
        return BukkitProject.BUKKIT_GROUP;
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{
            new MavenWizardStep(creator),
            new BukkitProjectSettingsWizardStep(creator)
        };
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new BukkitWizardStep(creator);
    }

    @Override
    public boolean validate(Project current, Project dest) {
        return true;
    }
}
