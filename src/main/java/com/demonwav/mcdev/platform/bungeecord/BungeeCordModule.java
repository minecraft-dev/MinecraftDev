package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.BukkitModule;
import com.demonwav.mcdev.platform.bungeecord.generation.BungeeCordGenerationData;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class BungeeCordModule extends AbstractModule {

    private VirtualFile pluginYml;

    BungeeCordModule(@NotNull Module module) {
        super(module);
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module).done(buildSystem -> pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE));
            }
        }
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    @Override
    public AbstractModuleType<BungeeCordModule> getModuleType() {
        return BungeeCordModuleType.getInstance();
    }

    public VirtualFile getPluginYml() {
        if (buildSystem == null) {
            buildSystem = BuildSystem.getInstance(module);
        }
        if (pluginYml == null && buildSystem != null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);
        }
        return pluginYml;
    }

    public void setPluginYml(VirtualFile pluginYml) {
        this.pluginYml = pluginYml;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.BUNGEECORD;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.BUNGEECORD_ICON;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return "net.md_5.bungee.api.plugin.Event".equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not a subclass of net.md_5.bungee.api.plugin.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public void doPreEventGenerate(@NotNull PsiClass psiClass, @Nullable GenerationData data) {
        final String bungeeCordListenerClass = "net.md_5.bungee.api.plugin.Listener";

        if (!McPsiUtil.extendsOrImplementsClass(psiClass, bungeeCordListenerClass)) {
            McPsiUtil.addImplements(psiClass, bungeeCordListenerClass, project);
        }
    }

    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        final String eventHandler = "net.md_5.bungee.event.EventHandler";
        final String eventPriority = "net.md_5.bungee.event.EventPriority";

        PsiMethod method = BukkitModule.generateBukkitStyleEventListenerMethod(
            containingClass,
            chosenClass,
            chosenName,
            project,
            eventHandler,
            false
        );

        BungeeCordGenerationData generationData = (BungeeCordGenerationData) data;
        if (generationData == null) {
            return method;
        }

        PsiModifierList modifierList = method.getModifierList();
        PsiAnnotation annotation = modifierList.findAnnotation(eventHandler);
        if (annotation == null) {
            return method;
        }

        if (generationData.getEventPriority().equals("NORMAL")) {
            return method;
        }

        PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
            .createExpressionFromText(eventPriority + "." + generationData.getEventPriority(), annotation);

        annotation.setDeclaredAttributeValue("priority", value);

        return method;
    }
}
