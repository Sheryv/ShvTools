package com.sheryv.tools.videoconverter

import com.sheryv.tools.videoconverter.cli.SwapCommand
import com.sheryv.tools.videoconverter.subtitles.EncodingConverterView
import com.sheryv.tools.videoconverter.subtitles.TranslatorView
import com.sheryv.tools.videoconverter.video.MainSettings
import com.sheryv.util.fx.core.app.App
import com.sheryv.util.fx.core.app.AppConfiguration
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.factoryOf
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess


object VideoConverterMain {
  const val NAME = "SHV Video Converter"
  
  @JvmStatic
  fun main(args: Array<String>) {
    val exitCode = CommandLine(RootCommand()).execute(*args)
    exitProcess(exitCode)
  }
  
  private fun runGui(spec: CommandLine.Model.CommandSpec) {
    App.createAndStart<VideoConverterView>(spec.args().map { it.toString() }.toTypedArray()) {
      module {
        factoryOf(::VideoConverterView)
        factory { Context(get()) }
        factoryOf(::EncodingConverterView)
        factoryOf(::TranslatorView)
        single(createdAtStart = true) { MainSettings.read() }
        single(createdAtStart = true) { Dispatchers.Main }
        single<AppConfiguration> { AppConfig() }
      }
    }
  }
  
  
  private class AppConfig : AppConfiguration() {
    override val name: String = NAME
  }
  
  
  @Command(
    name = "ShvVideoConverter",
    mixinStandardHelpOptions = true,
    version = ["1.0.0"],
    description = ["$NAME, a bulk video muxing utility"],
    subcommands = [SwapCommand::class, GuiCommand::class],
  )
  class RootCommand : Runnable {
    @CommandLine.Spec
    lateinit var spec: CommandLine.Model.CommandSpec
    
    override fun run() {
      runGui(spec)
    }
  }
  
  
  @Command(
    name = "gui",
    description = ["Launches GUI"]
  )
  class GuiCommand : Callable<Int> {
    @CommandLine.Spec
    lateinit var spec: CommandLine.Model.CommandSpec
    
    override fun call(): Int {
      runGui(spec)
      return 0
    }
  }
}


