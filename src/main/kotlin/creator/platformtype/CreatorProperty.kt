package com.demonwav.mcdev.creator.platformtype

import com.demonwav.mcdev.creator.custom.types.PropertyType
import com.intellij.openapi.observable.properties.GraphProperty

data class CreatorProperty<T>(val graphProperty: GraphProperty<T>, val type: PropertyType<T>)
