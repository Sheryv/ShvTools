package com.sheryv.tools.videoconverter.mergetomkv

import com.sheryv.util.fx.core.app.App
import com.sheryv.util.fx.core.app.AppConfiguration
import com.sheryv.util.fx.core.view.ViewFactory
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

object MergeStreamsToSingleMkvFile {
  
  @JvmStatic
  fun main(args: Array<String>) {
    App.createAndStart<MergeStreamsToSingleMkvView>(args) {
      module {
        factoryOf(::MergeStreamsToSingleMkvView)
        single(createdAtStart = true) { ConverterSettings.read() }
        single<AppConfiguration> { AppConfig() }
      }
    }
  }
  
  
  private class AppConfig : AppConfiguration() {
    override val name: String = "SHV Video Converter"
  }
}
