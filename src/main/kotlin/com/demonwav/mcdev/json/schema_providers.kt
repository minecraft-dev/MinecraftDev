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
        listOf(
            SoundsSchemaProvider(),
            PathBasedSchemaProvider("Minecraft Blockstates JSON", "blockstates", "blockstates/"),
            PathBasedSchemaProvider("Minecraft Item Model JSON", "model_item", "models/item/"),
            PathBasedSchemaProvider("Minecraft Block Model JSON", "model_block", "models/block/"),
            PathBasedSchemaProvider("Minecraft Loot Table JSON", "loot_table", "loot_tables/")
        )
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

class PathBasedSchemaProvider(name: String, schema: String, private val path: String) : JsonSchemaFileProvider {
    private val _name = name
    private val file = JsonSchemaProviderFactory.getResourceFile(SchemaProviderFactory::class.java, "/jsonSchemas/$schema.schema.json")

    override fun getName() = this._name

    override fun isAvailable(file: VirtualFile) = file.resourceDomain != null && file.resourcePath?.startsWith(path) == true

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaFile(): VirtualFile = file
}
