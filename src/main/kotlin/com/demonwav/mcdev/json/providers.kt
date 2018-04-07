/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.json

import com.demonwav.mcdev.util.resourceDomain
import com.demonwav.mcdev.util.resourcePath
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class SchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project) =
        listOf(SoundsSchemaProvider(), BlockstatesSchemaProvider(), ItemModelSchemaProvider())
}

class SoundsSchemaProvider : JsonSchemaFileProvider {
    companion object {
        val FILE = JsonSchemaProviderFactory.getResourceFile(SchemaProviderFactory::class.java, "/jsonSchemas/sounds.schema.json")
    }

    override fun getName() = "Minecraft Sounds JSON"

    override fun isAvailable(file: VirtualFile) = file.resourceDomain != null && file.resourcePath == "sounds.json"

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaFile(): VirtualFile = FILE
}

class BlockstatesSchemaProvider : JsonSchemaFileProvider {
    companion object {
        val FILE = JsonSchemaProviderFactory.getResourceFile(SchemaProviderFactory::class.java, "/jsonSchemas/blockstates.schema.json")
    }

    override fun getName() = "Minecraft Blockstates JSON"

    override fun isAvailable(file: VirtualFile) = file.resourceDomain != null && file.resourcePath?.startsWith("blockstates/") == true

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaFile(): VirtualFile = FILE
}

class ItemModelSchemaProvider : JsonSchemaFileProvider {
    companion object {
        val FILE = JsonSchemaProviderFactory.getResourceFile(SchemaProviderFactory::class.java, "/jsonSchemas/model_item.schema.json")
    }

    override fun getName() = "Minecraft Item Model JSON"

    override fun isAvailable(file: VirtualFile) = file.resourceDomain != null && file.resourcePath?.startsWith("models/item/") == true

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaFile(): VirtualFile = FILE
}
