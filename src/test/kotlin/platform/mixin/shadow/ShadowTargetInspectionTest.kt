/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.shadow

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationTargetInspection
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Shadow Target Inspection Tests")
class ShadowTargetInspectionTest : BaseMixinTest() {

    @Test
    @DisplayName("Shadow Target Inspection Test")
    fun shadowTargetInspectionTest() {
        buildProject {
            java(
                "ShadowData.java",
                """
                package test;

                import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
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

                    @<error descr="Unresolved field nonExistent in target class">Shadow</error> public String nonExistent;

                    @Shadow protected String twoIssues;
                }
                """,
            )
        }

        fixture.enableInspections(MixinAnnotationTargetInspection::class)
        fixture.checkHighlighting(true, false, false)
    }
}
