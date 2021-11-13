package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sheryv.tools.filematcher.utils.Hashing
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Hash(
  val md5: String? = null,
  val sha256: String? = null,
  val sha1: String? = null,
  val crc32: String? = null
) {
  
  fun hasAny() = md5 != null || sha256 != null || sha1 != null || crc32 != null
  
  @JsonIgnore
  fun getCorrespondingHasherAndCompare(): (File) -> Boolean {
    if (sha256 != null) {
      return { Hashing.sha256(it.toPath()) == sha256.uppercase() }
    }
    if (md5 != null) {
      return { Hashing.md5(it.toPath()) == md5.uppercase() }
    }
    if (sha1 != null) {
      return { Hashing.sha1(it.toPath()) == sha1.uppercase() }
    }
    if (crc32 != null) {
      return { Hashing.crc32(it.toPath()) == crc32.uppercase() }
    }
    throw IllegalStateException("No hash found")
  }
  
  fun hasAnySameHash(hash: Hash?): Boolean {
    return hash != null
        && (md5 != null && md5 == hash.md5
        || sha256 != null && sha256 == hash.sha256
        || sha1 != null && sha1 == hash.sha1
        || crc32 != null && crc32 == hash.crc32)
  }
}
