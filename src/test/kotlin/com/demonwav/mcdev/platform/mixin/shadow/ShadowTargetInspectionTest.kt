/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.shadow

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.shadow.ShadowTargetInspection
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Shadow Target Inspection Tests")
class ShadowTargetInspectionTest : BaseShadowTest() {

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

                    @Shadow public String wrongAccessor;
                    @Shadow protected String noFinal;

                    <error descr="Cannot resolve member 'nonExistent' in target class">@Shadow</error> public String nonExistent;

                    @Shadow protected String twoIssues;
                }
            """)
        }
    }

    @Test
    @DisplayName("Shadow Target Inspection Test")
    fun shadowTargetInspectionTest() {
        fixture.enableInspections(ShadowTargetInspection::class.java)
        fixture.checkHighlighting(true, false, false)
    }
}
