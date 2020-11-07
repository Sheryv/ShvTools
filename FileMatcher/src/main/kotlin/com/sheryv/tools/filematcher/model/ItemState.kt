package com.sheryv.tools.filematcher.model

enum class ItemState(val label: String, val cssClass: String, val processing: Boolean = false, val toModify: Boolean = false) {
  UNKNOWN("Unknown: Verification needed", "bg-purple"),
  VERIFICATION("Verification...", "bg-purple", true),
  DOWNLOADING("Downloading...", "bg-yellow", true),
  SYNCED("Synced", "bg-green"),
  MODIFIED("Modified locally: Ready to override", "bg-red", toModify = true),
  SKIPPED("Differs locally: Skipped", "bg-grey"),
  NOT_EXISTS("Does not exist: Ready to download", "bg-blue", toModify = true),
  NEEDS_UPDATE("Exists: Ready to override", "bg-orange", toModify = true),
  ;
  
  override fun toString(): String {
    return label
  }
}