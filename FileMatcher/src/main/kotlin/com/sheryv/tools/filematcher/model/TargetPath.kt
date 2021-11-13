package com.sheryv.tools.filematcher.model

data class TargetPath(
  val directory: BasePath? = null,
  val matching: Matching = Matching(),
  val absolute: Boolean = false,
  val override: Boolean = true,
) {


}
