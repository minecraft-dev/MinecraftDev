package com.demonwav.mcdev.creator.platformtype

import com.demonwav.mcdev.creator.custom.types.PropertyType
import com.intellij.openapi.observable.properties.ObservableProperty

data class CreatorProperty<T>(val graphProperty: ObservableProperty<T>, val type: PropertyType<T>)
