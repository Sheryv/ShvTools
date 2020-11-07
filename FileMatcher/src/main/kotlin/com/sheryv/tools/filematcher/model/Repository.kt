package com.sheryv.tools.filematcher.model

import java.time.OffsetDateTime

data class Repository(
    val baseUrl: String,
    val codeName: String = "",
    val repositoryVersion: String? = null,
    val schemaVersion: Long? = null,
    val website: String? = null,
    val title: String = "",
    val author: String? = null,
    val additionalFields: Map<String, String?> = emptyMap(),
    val updateDate: OffsetDateTime? = null,
    var bundles: List<Bundle> = emptyList()
)