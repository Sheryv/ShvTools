package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore

class SaveOptions(
  val bundleId: String = "bundle1",
  val bundleName: String = "Example bundle",
  val bundleUrl: String? = null,
  versionId: String = "1",
  val versionName: String = "0.1.0",
  val repoUrl: String = "http://localhost/",
  val repoName: String = "codeName",
  val repoTitle: String = "",
  val bundleBasePath: String = "\${user.home}",
  val format: String = "YAML",
  val splitVersionsToFiles: Boolean = false,
  val overrideExistingItems: Boolean = false
) {
  val versionId: Long
  
  init {
    ValidationResult()
      .assert(repoUrl.isNotBlank(), "Repository URL cannot be empty")
      .assert(repoName.isNotBlank(), "Repository name name cannot be empty")
      .assert(bundleId.isNotBlank(), "Bundle id cannot be empty")
      .assert(bundleName.isNotBlank(), "Bundle name cannot be empty")
      .assert(versionId.isNotBlank(), "Version id cannot be empty")
      .assert(versionId.toLongOrNull() != null, "Version id have to be integer")
      .assert(versionName.isNotBlank(), "Version name cannot be empty")
      .assert(bundleBasePath.isNotBlank(), "Bundle target dir is required. Fill it in text field in Options tab")
      .assert(FORMATS.contains(format), "Incorrect format $format")
      .throwIfError()
    this.versionId = versionId.toLong()
  }
  
  @JsonIgnore
  fun isJson() = "JSON" == format.uppercase()
  
  companion object {
    @JvmStatic
    val FORMATS = listOf("YAML", "JSON")
  }
}
