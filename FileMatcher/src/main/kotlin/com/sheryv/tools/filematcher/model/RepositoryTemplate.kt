package com.sheryv.tools.filematcher.model

import java.time.OffsetDateTime

data class RepositoryTemplate(
  val baseUrl: String,
  val codeName: String = "",
  val repositoryVersion: String? = null,
  val schemaVersion: Long? = null,
  val website: String? = null,
  val title: String = "",
  val author: String? = null,
  val description: String? = null,
  val additionalFields: Map<String, String?> = emptyMap(),
  val updateDate: OffsetDateTime? = null,
  var bundles: List<BundleTemplate> = emptyList()
) {
  
  constructor(r: Repository) : this(
    r.baseUrl,
    r.codeName,
    r.repositoryVersion,
    r.schemaVersion,
    r.website,
    r.title,
    r.author,
    r.description,
    r.additionalFields,
    r.updateDate,
    r.bundles.map { BundleTemplate(it) }
  )
  
  fun toRepo(filledBundles: List<Bundle>): Repository {
    return Repository(
      baseUrl,
      codeName,
      repositoryVersion,
      schemaVersion,
      website,
      title,
      author,
      description,
      additionalFields,
      updateDate,
      filledBundles
    )
  }
}
