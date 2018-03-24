/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.shadow

import com.demonwav.mcdev.platform.mixin.inspection.shadow.ShadowModifiersInspection

class ShadowModifiersInspectionTest : BaseShadowTest() {

    override fun createMixins() {
        mixins = {
            java("test/ShadowData.java", """
                package test;

                import org.spongepowered.asm.mixin.Mixin;
                import org.spongepowered.asm.mixin.Shadow;
                import org.spongepowered.asm.mixin.Final;

                @Mixin(MixinBase.class)
                public class ShadowData {
                    @Shadow @Final private String privateFinalString;
                    @Shadow private String privateString;

                    @Shadow @Final protected String protectedFinalString;
                    @Shadow protected String protectedString;

                    @Shadow @Final String packagePrivateFinalString;
                    @Shadow String packagePrivateString;

                    @Shadow @Final public String publicFinalString;
                    @Shadow public String publicString;

                    @Shadow <warning descr="Invalid access modifiers, has: public, but target member has: protected">public</warning> String wrongAccessor;
                    <warning descr="@Shadow for final member should be annotated as @Final">@Shadow</warning> protected String noFinal;

                    @Shadow public String nonExistent;

                    <warning descr="@Shadow for final member should be annotated as @Final">@Shadow</warning> <warning descr="Invalid access modifiers, has: protected, but target member has: public">protected</warning> String twoIssues;
                }
            """)
        }
    }

    fun `test shadow modifiers inspection`() {
        myFixture.enableInspections(ShadowModifiersInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }
}
