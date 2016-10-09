/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.insight.generation;

import com.demonwav.mcdev.insight.generation.ui.EventGenerationDialog;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringBundle;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The standard handler to generate a new event listener as a method.
 * Note that this is a psuedo generator as it relies on a wizard and the
 * {@link #cleanup()} to complete
 */
public class GenerateEventListenerHandler extends GenerateMembersHandlerBase {

    private static final PsiElementClassMember[] DUMMY_RESULT = new PsiElementClassMember[1]; //cannot return empty array, but this result won't be used anyway
    private Editor editor;
    private LogicalPosition position;
    private PsiMethod method;
    private CaretModel model;

    private GenerationData data;
    private PsiClass chosenClass;
    private String chosenName;
    private AbstractModule relevantModule;
    private boolean okay;

    public GenerateEventListenerHandler() {
        super("Generate Event Listener");
    }

    @Override
    protected String getHelpId() {
        return "Generate Event Listener Dialog";
    }


    @Override
    protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project, Editor editor) {
        this.editor = editor;

        final Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(aClass);
        if (moduleForPsiElement == null) {
            return null;
        }

        final MinecraftModule minecraftModule = MinecraftModule.getInstance(moduleForPsiElement);
        if (minecraftModule == null) {
            return null;
        }

        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                .createWithInnerClassesScopeChooser(RefactoringBundle.message("choose.destination.class"),
                        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(moduleForPsiElement, false),
                        aClass1 ->
                                isSuperEventListenerAllowed(aClass1, minecraftModule),
                        null
                );

        chooser.showDialog();
        chosenClass = chooser.getSelected();
        if (chosenClass == null) {
            return null;
        }

        Optional<AbstractModule> relevantModule = minecraftModule.getModules().stream()
            .filter(m -> isSuperEventListenerAllowed(chosenClass, m))
            .findFirst();

        if (relevantModule.isPresent()) {
            this.relevantModule = relevantModule.get();

            @SuppressWarnings("ConstantConditions")
            EventGenerationDialog generationDialog = new EventGenerationDialog(
                editor,
                relevantModule.get().getModuleType().getEventGenerationPanel(chosenClass),
                chosenClass.getNameIdentifier().getText(),
                relevantModule.get().getModuleType().getDefaultListenerName(chosenClass)
            );

            okay = generationDialog.showAndGet();

            if (okay) {
                data = generationDialog.getData();
                chosenName = generationDialog.getChosenName();

                model = editor.getCaretModel();
                position = model.getLogicalPosition();

                method = PsiTreeUtil.getParentOfType(aClass.getContainingFile().findElementAt(model.getOffset()), PsiMethod.class);
            } else {
                return null;
            }
        } else {
            return null;
        }

        return DUMMY_RESULT;
    }

    @Override
    protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
        return null;
    }

    @Override
    protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember) {
        if (!okay) {
            return null;
        }

        if (relevantModule != null) {
            relevantModule.doPreEventGenerate(aClass, data);

            model.moveToLogicalPosition(position);

            PsiMethod newMethod = relevantModule.generateEventListenerMethod(aClass, chosenClass, chosenName, data);

            if (newMethod != null) {
                PsiGenerationInfo<PsiMethod> info = new PsiGenerationInfo<>(newMethod);
                info.positionCaret(editor, true);
                if (method != null) {
                    info.insert(aClass, method, false);
                }

                return new GenerationInfo[]{
                    info
                };
            }
        }


        return null;
    }

    private static boolean isSuperEventListenerAllowed(PsiClass eventClass, MinecraftModule module) {
        final PsiClass[] supers = eventClass.getSupers();
        for (PsiClass aSuper : supers) {
            if (module.isEventClassValidForModule(aSuper)) {
                return true;
            }
            if (isSuperEventListenerAllowed(aSuper, module)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSuperEventListenerAllowed(PsiClass eventClass, AbstractModule module) {
        final PsiClass[] supers = eventClass.getSupers();
        for (PsiClass aSuper : supers) {
            if (module.isEventClassValid(aSuper, null)) {
                return true;
            }
            if (isSuperEventListenerAllowed(aSuper, module)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAvailableForQuickList(@NotNull Editor editor, @NotNull PsiFile file, @NotNull DataContext dataContext) {
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        if (module == null) {
            return false;
        }

        MinecraftModule instance = MinecraftModule.getInstance(module);
        return instance != null && instance.isEventGenAvailable();
    }
}
