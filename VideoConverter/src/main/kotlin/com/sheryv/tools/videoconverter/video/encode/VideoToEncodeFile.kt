package com.sheryv.tools.videoconverter.video.encode

import com.sheryv.tools.videoconverter.video.convert.ConversionProcessState
import com.sheryv.util.fx.lib.*
import java.nio.file.Path

class VideoToEncodeFile(val path: Path, quality: Int, state: ConversionProcessState, val outputPath: Path) {
  val qualityProperty = intProperty(quality)
  var pathProperty = staticProperty(path)
  
  val stateProperty = objectProperty(state)
  val state by stateProperty
  
  val progressProperty = doubleProperty(0.0)
}
