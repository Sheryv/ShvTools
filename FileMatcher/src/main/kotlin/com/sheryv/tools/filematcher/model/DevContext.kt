package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.BundleUtils
import java.io.File

class DevContext(
  val repo: Repository,
  val version: BundleVersion,
) {
  val bundle: Bundle = version.bundle
  val repoCopy: Repository = BundleUtils.copyRepo(repo)
  
  fun toUserContext(basePath: File? = null): UserContext {
    return UserContext(
      repo,
      version.bundle.id,
      version.versionId,
      basePath
    )
  }
}
