package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.filematcher.utils.DataUtils
import java.time.OffsetDateTime

class Bundle(
    val id: String,
    val name: String = "",
    val description: String = "",
    var versions: List<BundleVersion> = emptyList(),
    val type: BundleType = BundleType.BUNDLE,
    val versioningMode: BundleMode = BundleMode.AGGREGATE_OLD,
    val preferredBasePath: BasePath = BasePath(),
    val baseItemUrl: String? = null,
    val updateDate: OffsetDateTime? = null,
    val additionalFields: Map<String, String?> = emptyMap(),
    val link: String? = null,
    val linkedId: String? = null,
    val experimental: Boolean = false
) {
  
  @JsonIgnore
  fun getBaseUrl(repoBase: String?): String? {
    var res = repoBase?.trim('/')?.takeIf { baseItemUrl == null || !DataUtils.isAbsoluteUrl(baseItemUrl) }
    if (!baseItemUrl.isNullOrBlank()) {
      res = (res?.plus("/") ?: "") + baseItemUrl.trim('/')
    }
    return res
  }
  
  override fun toString(): String {
    return name
  }
}