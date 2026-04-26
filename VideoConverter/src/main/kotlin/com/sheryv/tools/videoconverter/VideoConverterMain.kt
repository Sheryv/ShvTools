package com.sheryv.tools.videoconverter

import com.sheryv.tools.videoconverter.subtitles.EncodingConverterView
import com.sheryv.tools.videoconverter.video.MainSettings
import com.sheryv.tools.videoconverter.subtitles.TranslatorView
import com.sheryv.util.fx.core.app.App
import com.sheryv.util.fx.core.app.AppConfiguration
import org.koin.core.module.dsl.factoryOf

object VideoConverterMain {
  
  @JvmStatic
  fun main(args: Array<String>) {
    App.createAndStart<VideoConverterView>(args) {
      module {
        factoryOf(::VideoConverterView)
        factoryOf(::EncodingConverterView)
        factoryOf(::TranslatorView)
        single(createdAtStart = true) { MainSettings.read() }
        single<AppConfiguration> { AppConfig() }
      }
    }
  }
  
  
  private class AppConfig : AppConfiguration() {
    override val name: String = "SHV Video Converter"
  }
}
