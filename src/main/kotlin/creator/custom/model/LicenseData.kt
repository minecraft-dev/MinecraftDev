package com.demonwav.mcdev.creator.custom.model

import java.time.ZonedDateTime

@TemplateApi
data class LicenseData(
    val id: String,
    val name: String,
    val year: String = ZonedDateTime.now().year.toString(),
)
