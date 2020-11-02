package com.sheryv.tools.filematcher.model

data class BundleVersion(
    val versionId: Long = 0,
    val versionName: String,
    val changesDescription: String = "",
    var entries: List<Entry> = emptyList(),
    val additionalFields: Map<String, String?> = emptyMap(),
    val experimental: Boolean = false
) {
  
  override fun toString(): String {
    return "$versionName [$versionId]"
  }
}