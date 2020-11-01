package com.sheryv.tools.filematcher.model

import java.time.OffsetDateTime

class Repository(
    val baseUrl: String,
    val codeName: String = "",
    val version: String? = null,
    val website: String? = null,
    val title: String = "",
    val author: String? = null,
    val additionalFields: Map<String, String?> = emptyMap(),
    val updateDate: OffsetDateTime? = null,
    var bundles: List<Bundle> = emptyList()
)