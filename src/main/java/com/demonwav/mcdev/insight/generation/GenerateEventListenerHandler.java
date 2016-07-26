package com.demonwav.mcdev.insight.generation;

import com.demonwav.mcdev.asset.MessageAssets;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.refactoring.util.classMembers.UsesAndInterfacesDependencyMemberInfoModel;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The standard handler to generate a new event listener as a method.
 * Note that this is a psuedo generator as it relies on a wizard and the
 * {@link #cleanup()} to complete
 */
public class GenerateEventListenerHandler extends GenerateMembersHandlerBase {

    private static final PsiElementClassMember[] DUMMY_RESULT = new PsiElementClassMember[1]; //cannot return empty array, but this result won't be used anyway
    private ReferenceEditorComboWithBrowseButton myTfTargetClassName;
    private PsiClass mySourceClass;
    private Project project;

    public GenerateEventListenerHandler() {
        super("Generate Event Listener");
    }

    @Override
    protected String getHelpId() {
        return "Generate Event Listener Dialog";
    }


    @Override
    protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project, Editor editor) {

        final Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(aClass);
        if (moduleForPsiElement == null) {
            return null;
        }

        final MinecraftModule minecraftModule = MinecraftModule.getInstance(moduleForPsiElement);
        if (minecraftModule == null) {
            return null;
        }


        this.project = project;

        myTfTargetClassName = new ReferenceEditorComboWithBrowseButton(new ChooseClassAction(), "Event", this.project, true, JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE, "");

        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                .createWithInnerClassesScopeChooser(RefactoringBundle.message("choose.destination.class"),
                        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(moduleForPsiElement, false),
                        aClass1 ->
                                isSuperEventListenerAllowed(aClass1, minecraftModule),
                        null
                );
        final String targetClassName = getTargetClassName();
        if (targetClassName != null) {
            final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(targetClassName, GlobalSearchScope.allScope(project));
            if (psiClass != null) {
                chooser.selectDirectory(psiClass.getContainingFile().getContainingDirectory());
            } else {
                chooser.selectDirectory(aClass.getContainingFile().getContainingDirectory());
            }
        }

        chooser.showDialog();
        PsiClass chosenClass = chooser.getSelected();

        if (chosenClass != null) {
            this.project = chosenClass.getProject();

            myTfTargetClassName.setText(chosenClass.getQualifiedName());
            if (chosenClass.getName() != null) {
                // TODO: delegate this to the platform, currently only working for bukkit
                PsiMethod newMethod = JavaPsiFacade.getElementFactory(project).createMethod("on" + chosenClass.getName().replace("Event", ""), PsiType.VOID);

                PsiParameterList list = newMethod.getParameterList();
                PsiParameter parameter = JavaPsiFacade.getElementFactory(project).createParameter("event", PsiClassType.getTypeByName(chosenClass.getQualifiedName(), project, GlobalSearchScope.moduleScope(moduleForPsiElement)));
                list.add(parameter);

                PsiModifierList modifierList = newMethod.getModifierList();
                PsiAnnotation annotation = modifierList.addAnnotation("org.bukkit.event.EventHandler");

                PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project).createExpressionFromText("true", annotation);

                annotation.setDeclaredAttributeValue("ignoreCancelled", value);


                return new PsiMethodMember[] { new PsiMethodMember(newMethod) };
            }
        }

        return DUMMY_RESULT;
    }

    public String getTargetClassName() {
        return myTfTargetClassName.getText();
    }

    @Override
    protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
        return null;
    }

    @Override
    protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember) {
        if (originalMember instanceof PsiMethodMember) {
            return new GenerationInfo[] {
                new PsiGenerationInfo<>(((PsiMethodMember) originalMember).getElement())
            };
        }
        return null;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
    }

    private class MyMemberInfoModel extends UsesAndInterfacesDependencyMemberInfoModel<PsiMember, MemberInfo> {
        PsiClass myTargetClass = null;
        public MyMemberInfoModel() {
            super(mySourceClass, null, false, DEFAULT_CONTAINMENT_VERIFIER);
        }

        @Override
        @Nullable
        public Boolean isFixedAbstract(MemberInfo member) {
            return null;
        }

        @Override
        public boolean isCheckedWhenDisabled(MemberInfo member) {
            return false;
        }

        @Override
        public boolean isMemberEnabled(MemberInfo member) {
            if(myTargetClass != null && myTargetClass.isInterface() && !PsiUtil.isLanguageLevel8OrHigher(myTargetClass)) {
                return !(member.getMember() instanceof PsiMethod);
            }
            return super.isMemberEnabled(member);
        }

        public void updateTargetClass() {
            final PsiManager manager = PsiManager.getInstance(project);
            myTargetClass =
                    JavaPsiFacade.getInstance(manager.getProject()).findClass(getTargetClassName(), GlobalSearchScope.projectScope(project));
        }
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

    private class ChooseClassAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                    .createWithInnerClassesScopeChooser(
                            MessageAssets.getGenerateEventListenerTitle(),
                            GlobalSearchScope.projectScope(project),
                            aClass ->
                                    (aClass.getParent() instanceof PsiFile
                                            || aClass.hasModifierProperty(PsiModifier.STATIC))
                                            && aClass.hasModifierProperty(PsiModifier.PUBLIC)
                            ,
                            null
                    );
            final String targetClassName = getTargetClassName();
            if (targetClassName != null) {
                final PsiClass aClass = JavaPsiFacade.getInstance(project)
                        .findClass(targetClassName, GlobalSearchScope.allScope(project));
                if (aClass != null) {
                    chooser.selectDirectory(aClass.getContainingFile().getContainingDirectory());
                } else {
                    chooser.selectDirectory(mySourceClass.getContainingFile().getContainingDirectory());
                }
            }

            chooser.showDialog();
            PsiClass aClass = chooser.getSelected();
            if (aClass != null) {
                myTfTargetClassName.setText(aClass.getQualifiedName());
            }
        }
    }
}
