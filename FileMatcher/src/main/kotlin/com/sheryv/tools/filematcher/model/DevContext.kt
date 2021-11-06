package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.BundleUtils

class DevContext(
  val repo: Repository,
  val version: BundleVersion,
) {
  val bundle: Bundle = version.bundle
  val repoCopy: Repository = BundleUtils.copyRepo(repo)
}
