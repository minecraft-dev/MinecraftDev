package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.MinecraftCodeInsightFixtureTestCase
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import org.junit.Assert

class ShadowTest : MinecraftCodeInsightFixtureTestCase() {

    private val path = "src/test/resources/com/demonwav/mcdev/platform/mixin/fixture"
    private lateinit var psiClass: PsiClass

    override fun getTestDataPath(): String {
        return path
    }

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        val psiFiles = myFixture.configureByFiles("src/test/ShadowData.java", "src/test/MixinBase.java")
        psiClass = (psiFiles[0] as PsiJavaFile).classes[0]
    }

    private fun checkShadow(element: PsiElement?, targets: Int, errors: Int) {
        val shadowedElement = MixinUtils.getShadowedElement(element)
        Assert.assertNotNull(shadowedElement)
        Assert.assertEquals(targets, shadowedElement.targets.size)
        Assert.assertEquals(errors, shadowedElement.errors.size)
    }

    private fun checkGoodShadow(element: PsiElement?) {
        checkShadow(element, 1, 0)
    }

    private fun checkBadShadow(element: PsiElement?) {
        checkShadow(element, 1, 1)
    }

    private fun checkReallyBadShadow(element: PsiElement?) {
        checkShadow(element, 0, 1)
    }

    // Good shadows
    fun testPrivateFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("privateFinalString", false))
    }

    fun testPrivateShadow() {
        checkGoodShadow(psiClass.findFieldByName("privateString", false))
    }

    fun testProtectedFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("protectedFinalString", false))
    }

    fun testProtectedShadow() {
        checkGoodShadow(psiClass.findFieldByName("protectedString", false))
    }

    fun testPackagePrivateFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("packagePrivateFinalString", false))
    }

    fun testPackagePrivateShadow() {
        checkGoodShadow(psiClass.findFieldByName("packagePrivateString", false))
    }

    fun testPublicFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("publicFinalString", false))
    }

    fun testPublicShadow() {
        checkGoodShadow(psiClass.findFieldByName("publicString", false))
    }

    // Bad shadows
    fun testWrongAccessor() {
        checkBadShadow(psiClass.findFieldByName("wrongAccessor", false))
    }

    fun testNoFinal() {
        checkBadShadow(psiClass.findFieldByName("noFinal", false))
    }

    fun testTwoIssues() {
        checkShadow(psiClass.findFieldByName("twoIssues", false), 1, 2)
    }

    // Really bad shadows
    fun testNonExistent() {
        checkReallyBadShadow(psiClass.findFieldByName("nonExistent", false))
    }

}