/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation

import com.intellij.codeInsight.generation.actions.BaseGenerateAction

class GenerateEventListenerAction : BaseGenerateAction(GenerateEventListenerHandler())
