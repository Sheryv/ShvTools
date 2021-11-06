package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.OffsetDateTime

data class BundleTemplate(
  var id: String,
  var name: String = "",
  var description: String = "",
  var versioningMode: BundleMode = BundleMode.ONLY_SELECTED,
  var preferredBasePath: BasePath = BasePath(),
  var baseItemUrl: String? = null,
  var updateDate: OffsetDateTime? = null,
  var additionalFields: Map<String, String?> = emptyMap(),
  @JsonIgnore
  var experimental: Boolean = false,
  var versions: List<BundleVersionBase>? = null,
  var link: String? = null,
  var linkedId: String? = null,
) {
  
  @JsonIgnore
  var specSource: String? = null
  
  constructor(b: Bundle) : this(
    b.id,
    b.name,
    b.description,
    b.versioningMode,
    b.preferredBasePath,
    b.baseItemUrl,
    b.updateDate,
    b.additionalFields,
    b.experimental,
    b.versions,
  )
  
  override fun toString(): String {
    return name
  }
  
  fun isLink() = link != null
  
  fun toBundle(versionsFilled: List<BundleVersion>): Bundle {
    val b = Bundle(
      id,
      name,
      description,
      versionsFilled,
      versioningMode,
      preferredBasePath,
      baseItemUrl,
      updateDate,
      additionalFields,
      experimental
    )
    b.specSource = specSource
    return b
  }
  
  fun toLink(): BundleLink {
    return BundleLink(
      id,
      link!!,
      name,
    )
  }
}
