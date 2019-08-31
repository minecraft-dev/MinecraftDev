/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.CommenterTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.platform.PlatformType
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Access Transformer Commenter Tests")
class AtCommenterTest : CommenterTest(PlatformType.MCP) {

    private fun doTest(
        @Language("Access Transformers") before: String,
        @Language("Access Transformers") after: String
    ) {
        doTest(before, after, "_at.cfg", ProjectBuilder::at)
    }

    @Test
    @DisplayName("Single Line Comment Test")
    fun singleLineCommentTest() = doTest("""
        public net.mine<caret>craft.entity.Entity field_190534_ay # fire
        public net.minecraft.entity.Entity field_70152_a # nextEntityID
    """, """
        #public net.minecraft.entity.Entity field_190534_ay # fire
        public net.minec<caret>raft.entity.Entity field_70152_a # nextEntityID
    """)

    @Test
    @DisplayName("Multi Line Comment Test")
    fun multiLineCommentTest() = doTest("""
        public net.minecraft.command.CommandHandler func<selection>_71559_a([Ljava/lang/String;)[Ljava/lang/String; # dropFirstString
        public net.minecraft.command.CommandHandler func_82370_a(Lnet/minecraft/command/ICommand;[Ljava/lang</selection>/String;)I # getUsernameIndex
        public net.minecraft.command.EntitySelector func_82381_h(Ljava/lang/String;)Ljava/util/Map; # getArgumentMap
    """, """
        #public net.minecraft.command.CommandHandler func<selection>_71559_a([Ljava/lang/String;)[Ljava/lang/String; # dropFirstString
        #public net.minecraft.command.CommandHandler func_82370_a(Lnet/minecraft/command/ICommand;[Ljava/lang</selection>/String;)I # getUsernameIndex
        public net.minecraft.command.EntitySelector func_82381_h(Ljava/lang/String;)Ljava/util/Map; # getArgumentMap
    """)

    @Test
    @DisplayName("Single Line Uncomment Test")
    fun singleLineUncommentTest() = doTest("""
        public net.minecraft.entity.Entity field_70152_a # nextEntityID
        public net.<caret>minecraft.entity.Entity func_190531_bD()I
        #public net.minecraft.entity.EntityHanging func_174859_a(Lnet/minecraft/util/EnumFacing;)V # updateFacingWithBoundingBox
        #public net.minecraft.entity.EntityList field_180126_g # stringToIDMapping
    """, """
        public net.minecraft.entity.Entity field_70152_a # nextEntityID
        #public net.minecraft.entity.Entity func_190531_bD()I
        #public net.<caret>minecraft.entity.EntityHanging func_174859_a(Lnet/minecraft/util/EnumFacing;)V # updateFacingWithBoundingBox
        #public net.minecraft.entity.EntityList field_180126_g # stringToIDMapping
    """)

    @Test
    @DisplayName("Multi Line Uncomment")
    fun multiLineUncommentTest() = doTest("""
        #public net.minecraft<selection>.entity.EntityLivingBase field_70752_e # potionsNeedUpdate
        #publi</selection>c net.minecraft.entity.EntityLivingBase field_70755_b # entityLivingToAttack
        public net.minecraft.entity.EntityLivingBase func_184583_d(Lnet/minecraft/util/DamageSource;)Z # canBlockDamageSource
    """, """
        public net.minecraft<selection>.entity.EntityLivingBase field_70752_e # potionsNeedUpdate
        publi</selection>c net.minecraft.entity.EntityLivingBase field_70755_b # entityLivingToAttack
        public net.minecraft.entity.EntityLivingBase func_184583_d(Lnet/minecraft/util/DamageSource;)Z # canBlockDamageSource
    """)

    @Test
    @DisplayName("Multi Line Comment With Comments Test")
    fun multiLineCommentWithCommentsTest() = doTest("""
        public net.minecraft.entity.EntityLivingBase field_184621_as # HAND_STATES
        #public net.minecraft.ent<selection>ity.EntityLivingBase field_184632_c # HEALTH
        public net.minecraft.entity.EntityLivingBase field_184633_f # POTION_EFFECTS
        #public net.minecraft.entity.EntityLivingBase field_184634_g # HIDE_PARTICLES
        #public net.minecraft.entity.EntityLivingBase field_184635_h # </selection>ARROW_COUNT_IN_ENTITY
    """, """
        public net.minecraft.entity.EntityLivingBase field_184621_as # HAND_STATES
        ##public net.minecraft.ent<selection>ity.EntityLivingBase field_184632_c # HEALTH
        #public net.minecraft.entity.EntityLivingBase field_184633_f # POTION_EFFECTS
        ##public net.minecraft.entity.EntityLivingBase field_184634_g # HIDE_PARTICLES
        ##public net.minecraft.entity.EntityLivingBase field_184635_h # </selection>ARROW_COUNT_IN_ENTITY
    """)
}
