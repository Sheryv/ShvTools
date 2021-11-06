package com.sheryv.tools.filematcher.model

class BundleVersionLink(
  versionId: Long,
  val link: String,
  versionName: String = "",
//  val linkedId: String? = null,
) : BundleVersionBase(versionId, versionName) {

}
