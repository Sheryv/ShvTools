package com.sheryv.tools.videoconverter.mergetomkv

enum class ConversionProcessState(val label: String) {
  READY("Ready"),
  IN_QUEUE("In queue"),
  PROCESSING("Processing"),
  COMPLETED("Completed"),
  FAILED("Failed"),
  EXISTS("Already exists"),
  ;
  override fun toString(): String = label
}
