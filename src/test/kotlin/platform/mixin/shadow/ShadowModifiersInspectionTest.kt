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
import com.demonwav.mcdev.platform.mixin.inspection.shadow.ShadowModifiersInspection
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Shadow Modifiers Inspection Tests")
class ShadowModifiersInspectionTest : BaseMixinTest() {

    @Test
    @DisplayName("Shadow Modifiers Inspection Test")
    fun shadowModifiersInspectionTest() {
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

                    @Shadow <warning descr="Invalid access modifiers, has: public, but target member has: protected">public</warning> String wrongAccessor;
                    <warning descr="@Shadow for final member should be annotated as @Final">@Shadow</warning> protected String noFinal;

                    @Shadow public String nonExistent;

                    <warning descr="@Shadow for final member should be annotated as @Final">@Shadow</warning> <warning descr="Invalid access modifiers, has: protected, but target member has: public">protected</warning> String twoIssues;
                }
                """,
            )
        }

        fixture.enableInspections(ShadowModifiersInspection::class)
        fixture.checkHighlighting(true, false, false)
    }
}
