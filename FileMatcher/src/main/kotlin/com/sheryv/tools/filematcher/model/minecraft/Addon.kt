package com.sheryv.tools.filematcher.model.minecraft

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
class Addon(
    val addonID: Long,
    val installedFile: ModPackFile,
    val dateInstalled: OffsetDateTime? = null,
    val dateUpdated: OffsetDateTime? = null
) {
}