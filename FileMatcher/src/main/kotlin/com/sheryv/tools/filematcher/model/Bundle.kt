package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.lg
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime

class Bundle(
  id: String,
  name: String = "",
  val description: String = "",
  val versions: List<BundleVersion> = emptyList(),
  val versioningMode: BundleMode = BundleMode.ONLY_SELECTED,
  val preferredBasePath: BasePath = BasePath(),
  val baseItemUrl: String? = null,
  val updateDate: OffsetDateTime? = null,
  val additionalFields: Map<String, String?> = emptyMap(),
  @JsonIgnore
  val experimental: Boolean = false
) : BundleBase(id, name) {
  
  init {
    fillVersionsData()
  }
  
  @JsonIgnore
  var specSource: String? = null
  
  fun fillVersionsData() {
    versions.forEach { it.bundle = this }
  }
  
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
  
  fun toUniqueString(): String {
    return "$name ($id)"
  }
  
  fun copy(
    id: String = this.id,
    name: String = this.name,
    description: String = this.description,
    versions: List<BundleVersion> = this.versions.map { it.deepCopy() },
    versioningMode: BundleMode = this.versioningMode,
    preferredBasePath: BasePath = this.preferredBasePath.copy(),
    baseItemUrl: String? = this.baseItemUrl,
    updateDate: OffsetDateTime? = this.updateDate,
    additionalFields: Map<String, String?> = this.additionalFields.toMap(),
    experimental: Boolean = this.experimental
  ): Bundle {
    return Bundle(
      id,
      name,
      description,
      versions,
      versioningMode,
      preferredBasePath,
      baseItemUrl,
      updateDate,
      additionalFields,
      experimental
    )
  }
}
