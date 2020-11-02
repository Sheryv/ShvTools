package com.sheryv.tools.filematcher.model

enum class ItemState(val label: String, val cssClass: String) {
  UNKNOWN("Unknown: Verification needed", "bg-purple"),
  VERIFICATION("Verification...", "bg-purple"),
  DOWNLOADING("Downloading...", "bg-yellow"),
  SYNCED("Synced", "bg-green"),
  MODIFIED("Modified locally: Ready to override", "bg-red"),
  SKIPPED("Differs locally: Skipped", "bg-grey"),
  NOT_EXISTS("Does not exist: Ready to download", "bg-blue"),
  NEEDS_UPDATE("Exists: Ready to override", "bg-orange"),
  ;
  
  override fun toString(): String {
    return label
  }
}