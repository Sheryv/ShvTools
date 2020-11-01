package com.sheryv.tools.filematcher.model

enum class ItemState(val label: String) {
  UNKNOWN("Unknown: Verification needed"),
  VERIFICATION("Verification..."),
  DOWNLOADING("Downloading..."),
  SYNCED("Synced"),
  MODIFIED("Modified locally: Ready to override"),
  SKIPPED("Differs locally: Skipped"),
  NOT_EXISTS("Does not exist: Ready to download"),
  NEEDS_UPDATE("Exists: Ready to override"),
  ;
  
  override fun toString(): String {
    return label
  }
}