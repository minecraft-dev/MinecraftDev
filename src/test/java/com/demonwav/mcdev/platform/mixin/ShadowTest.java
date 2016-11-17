/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin;

import com.demonwav.mcdev.MinecraftCodeInsightFixtureTestCase;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

public class ShadowTest extends MinecraftCodeInsightFixtureTestCase {

    private PsiClass psiClass;

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/com/demonwav/mcdev/platform/mixin/fixture";
    }

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureByFiles("src/test/ShadowData.java", "src/test/MixinBase.java");
        final PsiFile shadowData = PsiDocumentManager.getInstance(getProject()).getPsiFile(getEditor().getDocument());

        final PsiJavaFile javaFile = (PsiJavaFile) shadowData;
        psiClass = javaFile.getClasses()[0];

    }

    private void checkShadow(PsiElement element) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(1, shadowedElement.getTargets().size());
        assertEquals(0, shadowedElement.getErrors().size());
    }

    private void checkBadShadow(PsiElement element) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(1, shadowedElement.getTargets().size());
        assertEquals(1, shadowedElement.getErrors().size());
    }

    private void checkReallyBadShadow(PsiElement element) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(0, shadowedElement.getTargets().size());
        assertEquals(1, shadowedElement.getErrors().size());
    }

    // Good shadows
    public void testPrivateFinalShadow() {
        checkShadow(psiClass.findFieldByName("privateFinalString", false));
    }

    public void testPrivateShadow() {
        checkShadow(psiClass.findFieldByName("privateString", false));
    }

    public void testProtectedFinalShadow() {
        checkShadow(psiClass.findFieldByName("protectedFinalString", false));
    }

    public void testProtectedShadow() {
        checkShadow(psiClass.findFieldByName("protectedString", false));
    }

    public void testPackagePrivateFinalShadow() {
        checkShadow(psiClass.findFieldByName("packagePrivateFinalString", false));
    }

    public void testPackagePrivateShadow() {
        checkShadow(psiClass.findFieldByName("packagePrivateString", false));
    }

    public void testPublicFinalShadow() {
        checkShadow(psiClass.findFieldByName("publicFinalString", false));
    }

    public void testPublicShadow() {
        checkShadow(psiClass.findFieldByName("publicString", false));
    }

    // Bad shadows
    public void testWrongAccessor() {
        checkBadShadow(psiClass.findFieldByName("wrongAccessor", false));
    }

    public void testNoFinal() {
        checkBadShadow(psiClass.findFieldByName("noFinal", false));
    }

    // Really bad shadows
    public void testNonExistent() {
        checkReallyBadShadow(psiClass.findFieldByName("nonExistent", false));
    }
}
