/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.framework.CommenterTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Access Widener Commenter Tests")
class AwCommenterTest : CommenterTest() {

    private fun doTest(
        @Language("Access Widener") before: String,
        @Language("Access Widener") after: String,
    ) {
        doTest(before, after, ".accesswidener", ProjectBuilder::aw)
    }

    @Test
    @DisplayName("Single Line Comment Test")
    fun singleLineCommentTest() = doTest(
        """
        accessWidener v2 named
        accessible field net/mine<caret>craft/entity/Entity fire Z
        accessible field net/minecraft/entity/Entity nextEntityID I
        """,
        """
        accessWidener v2 named
        #accessible field net/minecraft/entity/Entity fire Z
        accessible field net/minec<caret>raft/entity/Entity nextEntityID I
        """,
    )

    @Test
    @DisplayName("Multi Line Comment Test")
    fun multiLineCommentTest() = doTest(
        """
        accessWidener v2 named
        accessible method net/minecraft/command/CommandHandler dropFir<selection>stString ([Ljava/lang/String;)[Ljava/lang/String;
        accessible method net/minecraft/command/CommandHandler getUsernameIndex (Lnet/minecraft/command/ICommand;[Ljava/lang</selection>/String;)I
        accessible method net/minecraft/command/EntitySelector getArgumentMap (Ljava/lang/String;)Ljava/util/Map;
        """,
        """
        accessWidener v2 named
        #accessible method net/minecraft/command/CommandHandler dropFir<selection>stString ([Ljava/lang/String;)[Ljava/lang/String;
        #accessible method net/minecraft/command/CommandHandler getUsernameIndex (Lnet/minecraft/command/ICommand;[Ljava/lang</selection>/String;)I
        accessible method net/minecraft/command/EntitySelector getArgumentMap (Ljava/lang/String;)Ljava/util/Map;
        """,
    )

    @Test
    @DisplayName("Single Line Uncomment Test")
    fun singleLineUncommentTest() = doTest(
        """
        accessible field net/minecraft/entity/Entity nextEntityID I
        accessible method net/<caret>minecraft/entity/Entity func_190531_bD ()I
        #accessible method net/minecraft/entity/EntityHanging updateFacingWithBoundingBox (Lnet/minecraft/util/EnumFacing;)V
        #accessible field net/minecraft/entity/EntityList stringToIDMapping Ljava/util/Map;
        """,
        """
        accessible field net/minecraft/entity/Entity nextEntityID I
        #accessible method net/minecraft/entity/Entity func_190531_bD ()I
        #accessible method net/<caret>minecraft/entity/EntityHanging updateFacingWithBoundingBox (Lnet/minecraft/util/EnumFacing;)V
        #accessible field net/minecraft/entity/EntityList stringToIDMapping Ljava/util/Map;
        """,
    )

    @Test
    @DisplayName("Multi Line Uncomment")
    fun multiLineUncommentTest() = doTest(
        """
        #accessible field net/minecraft<selection>/entity/EntityLivingBase potionsNeedUpdate Z
        #accessi</selection>ble field net/minecraft/entity/EntityLivingBase entityLivingToAttack Lnet/minecraft/entity/EntityLivingBase;
        accessible method net/minecraft/entity/EntityLivingBase canBlockDamageSource (Lnet/minecraft/util/DamageSource;)Z
        """,
        """
        accessible field net/minecraft<selection>/entity/EntityLivingBase potionsNeedUpdate Z
        accessi</selection>ble field net/minecraft/entity/EntityLivingBase entityLivingToAttack Lnet/minecraft/entity/EntityLivingBase;
        accessible method net/minecraft/entity/EntityLivingBase canBlockDamageSource (Lnet/minecraft/util/DamageSource;)Z
        """,
    )

    @Test
    @DisplayName("Multi Line Comment With Comments Test")
    fun multiLineCommentWithCommentsTest() = doTest(
        """
        accessible field net/minecraft/entity/EntityLivingBase HAND_STATES I
        #accessible field net/minecraft/ent<selection>ity/EntityLivingBase HEALTH F
        accessible field net/minecraft/entity/EntityLivingBase POTION_EFFECTS Ljava/util/List; 
        #accessible field net/minecraft/entity/EntityLivingBase HIDE_PARTICLES Z
        #accessible field net/minecraft/entity/EntityLivingBase ARROW_COUNT_IN_ENTITY # </selection>Some comment
        """,
        """
        accessible field net/minecraft/entity/EntityLivingBase HAND_STATES I
        ##accessible field net/minecraft/ent<selection>ity/EntityLivingBase HEALTH F
        #accessible field net/minecraft/entity/EntityLivingBase POTION_EFFECTS Ljava/util/List; 
        ##accessible field net/minecraft/entity/EntityLivingBase HIDE_PARTICLES Z
        ##accessible field net/minecraft/entity/EntityLivingBase ARROW_COUNT_IN_ENTITY # </selection>Some comment
        """,
    )
}
