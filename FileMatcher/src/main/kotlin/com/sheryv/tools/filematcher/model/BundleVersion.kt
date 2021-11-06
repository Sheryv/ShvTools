package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import java.io.File
import java.nio.file.Path
import java.time.OffsetDateTime

class BundleVersion(
  versionId: Long,
  versionName: String,
  val versionUrlPart: String? = SystemUtils.encodeNameForWeb(versionName),
  val changesDescription: String = "",
  var entries: List<Entry> = emptyList(),
  val additionalFields: Map<String, String?> = emptyMap(),
  val updateDate: OffsetDateTime? = null,
  val experimental: Boolean = false,
  @JsonIgnore
  var specSource: String? = null
) : BundleVersionBase(versionId, versionName) {
  
  @JsonIgnore
  lateinit var bundle: Bundle
    internal set
  
  @JsonIgnore
  fun getAggregatedEntries(): List<Entry> {
    val result = entries.toMutableList()
    if (bundle.versioningMode == BundleMode.AGGREGATE_OLD) {
      val previous = bundle.versions.filter { it.versionId < versionId }.sortedByDescending { it.versionId }
      previous.forEach { v ->
        v.entries.forEach { e ->
          if (result.none { it.id == e.id }) {
            result.add(e)
          }
        }
      }
    }
    return result
  }
  
  @JsonIgnore
  fun getParentEntry(parentId: String): Entry {
    return findEntry(parentId) ?: throw IllegalArgumentException("Parent entry not found in context: $parentId")
  }
  
  @JsonIgnore
  fun buildDirPathForEntry(entry: Entry, basePath: File? = null): Path {
    if (entry.parent != null) {
      return basePath!!.toPath().resolve(relativePathWithParents(entry))
    }
    return basePath!!.toPath()
  }
  
  @JsonIgnore
  fun buildDirPathForEntry(entryId: String, basePath: File? = null): Path {
    return findEntry(entryId)?.let { buildDirPathForEntry(it, basePath) }
      ?: throw IllegalArgumentException("Entry not found in context: $entryId")
  }
  
  @JsonIgnore
  fun relativePathWithParents(entry: Entry, entries: List<Entry> = this.entries): Path {
    val parents = if (entry.parent != null) {
      val parts =
        BundleUtils.getParents(entry.parent, entries).mapNotNull { it.target.directory?.findPath() }.reversed()
      Path.of(parts.first(), *parts.drop(1).toTypedArray())
    } else Path.of("")
    
    return parents.resolve(entry.target.directory?.findPath() ?: "").resolve(entry.name)
  }
  
  @JsonIgnore
  fun relativePathWithParents(entryId: String, entries: List<Entry> = this.entries): Path {
    return findEntry(entryId)?.let { relativePathWithParents(it, entries) }
      ?: throw IllegalArgumentException("Entry not found in context: $entryId")
  }
  
  fun findEntry(id: String) = getAggregatedEntries().firstOrNull { it.id == id }
  
  override fun toString(): String {
    return toUniqueString()
  }
  
  fun toUniqueString(): String {
    return "$versionName ($versionId)"
  }
  
  fun copy(
    versionId: Long = this.versionId,
    versionName: String = this.versionName,
    versionUrlPart: String? = this.versionUrlPart,
    changesDescription: String = this.changesDescription,
    entries: List<Entry> = this.entries,
    additionalFields: Map<String, String?> = this.additionalFields,
    updateDate: OffsetDateTime? = this.updateDate,
    experimental: Boolean = this.experimental,
    specSource: String? = this.specSource,
  ): BundleVersion {
    return BundleVersion(
      versionId,
      versionName,
      versionUrlPart,
      changesDescription,
      entries,
      additionalFields,
      updateDate,
      experimental,
      specSource
    )
  }
  
  fun deepCopy(
    versionId: Long = this.versionId,
    versionName: String = this.versionName,
    versionUrlPart: String? = this.versionUrlPart,
    changesDescription: String = this.changesDescription,
    entries: List<Entry> = this.entries.map { it.copy() },
    additionalFields: Map<String, String?> = this.additionalFields.toMap(),
    updateDate: OffsetDateTime? = this.updateDate,
    experimental: Boolean = this.experimental,
    specSource: String? = this.specSource,
  ): BundleVersion {
    return BundleVersion(
      versionId,
      versionName,
      versionUrlPart,
      changesDescription,
      entries,
      additionalFields,
      updateDate,
      experimental,
      specSource
    )
  }
  
}
