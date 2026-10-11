package com.sheryv.tools.videoconverter.video.encode

import com.sheryv.util.fx.lib.getValue
import com.sheryv.util.fx.lib.intProperty
import com.sheryv.util.fx.lib.objectProperty
import com.sheryv.util.fx.lib.stringProperty
import java.nio.file.Path

class VideoReEncodingSettings(
  baseSearchPath: Path,
  globPathFilter: String,
  cliPath: Path,
  defaultQuality: Int,
  outputPath: Path
) {
  
  val baseSearchPathProperty = objectProperty(baseSearchPath)
  val globPathFilterProperty = stringProperty(globPathFilter)
  val cliPathProperty = objectProperty(cliPath)
  val defaultQualityProperty = intProperty(defaultQuality) //  from 50 to 0
  val outputPathProperty = objectProperty(outputPath)
  
  
  val baseSearchPath by baseSearchPathProperty
  val globPathFilter by globPathFilterProperty
  val cliPath by cliPathProperty
  val defaultQuality by defaultQualityProperty //  from 50 to 0
  val outputPath by outputPathProperty
  
  companion object {
    val empty = VideoReEncodingSettings(Path.of(""), "**.mkv", Path.of("handbrake.exe"), 24, Path.of("output"))
  }
}
