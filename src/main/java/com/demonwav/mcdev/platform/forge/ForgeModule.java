package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class ForgeModule extends AbstractModule {

    private VirtualFile mcmod;

    ForgeModule(@NotNull Module module) {
        this.module = module;
        this.buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module);
            }

            if (buildSystem != null) {
                mcmod = buildSystem.findFile("mcmod.info", SourceType.RESOURCE);
            }
        }
    }

    @Override
    public ForgeModuleType getModuleType() {
        return ForgeModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.FORGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        PsiAnnotation annotation = method.getModifierList().findAnnotation("net.minecraftforge.fml.common.Mod.EventHandler");
        if (annotation != null) {
            return "net.minecraftforge.fml.common.event.FMLEvent".equals(eventClass.getQualifiedName());
        }

        annotation = method.getModifierList().findAnnotation("net.minecraftforge.fml.common.eventhandler.SubscribeEvent");
        if (annotation != null) {
            return "net.minecraftforge.fml.common.eventhandler.Event".equals(eventClass.getQualifiedName());
        }

        // just default to true
        return true;
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        PsiAnnotation annotation = method.getModifierList().findAnnotation("net.minecraftforge.fml.common.Mod.EventHandler");

        if (annotation != null) {
            return "Parameter is not a subclass of net.minecraftforge.fml.common.event.FMLEvent\n" +
                    "Compiling and running this listener may result in a runtime exception";
        }

        return "Parameter is not a subclass of net.minecraftforge.fml.common.eventhandler.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

    public VirtualFile getMcmod() {
        return mcmod;
    }
}
