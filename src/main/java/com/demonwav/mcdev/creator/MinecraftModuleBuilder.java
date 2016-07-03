package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
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

import java.io.File;

import javax.swing.Icon;

public class MinecraftModuleBuilder extends JavaModuleBuilder {

    private MinecraftProjectCreator creator = new MinecraftProjectCreator();
    private final ProjectSettingsWizardStep[] steps = new ProjectSettingsWizardStep[] {
            new ProjectSettingsWizardStep(creator), // Bukkit, Spigot, Paper
            new ProjectSettingsWizardStep(creator), // Sponge
            new ProjectSettingsWizardStep(creator), // Forge
            new ProjectSettingsWizardStep(creator)  // BungeeCord
    };

    @Override
    public String getPresentableName() {
        return "Minecraft Plugin";
    }

    @Override
    public Icon getNodeIcon() {
        return PlatformAssets.MINECRAFT_ICON;
    }

    @Override
    public String getGroupName() {
        return "Minecraft Plugin";
    }

    @Override
    public int getWeight() {
        return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT - 1;
    }

    @Nullable
    @Override
    public String getBuilderId() {
        return "MINECRAFT_MODULE";
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdk) {
        return sdk == JavaSdk.getInstance();
    }

    @Override
    public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        final Project project = modifiableRootModel.getProject();
        final VirtualFile root = createAndGetRoot();
        if (root == null) {
            return;
        }
        modifiableRootModel.addContentEntry(root);

        if (getModuleJdk() != null) {
            modifiableRootModel.setSdk(getModuleJdk());
        }

        creator.setRoot(root);
        creator.setModule(modifiableRootModel.getModule());

        DumbAwareRunnable r = creator::create;

        if (project.isDisposed()) {
            return;
        }

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

        if (temp == null) {
            return null;
        }

        String path = FileUtil.toSystemIndependentName(temp);
        //noinspection ResultOfMethodCallIgnored
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    @Override
    public ModuleType getModuleType() {
        return JavaModuleType.getModuleType();
    }

    @Override
    public String getParentGroup() {
        return "Minecraft Project";
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[] {
                new SpongeForgeChooser(creator),
                new BuildSystemWizardStep(creator),
                // Due to this not allow dynamic steps at runtime, we just fill out all of them and skip the ones we don't use
                steps[0], // Bukkit, Spigot, Paper
                steps[1], // Sponge
                steps[2], // Forge
                steps[3]  // BungeeCord
        };
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new ProjectChooserWizardStep(creator, steps);
    }

    @Override
    public boolean validate(Project current, Project dest) {
        return true;
    }
}
