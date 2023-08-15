package com.sheryv.util.fx.core.app

import com.sheryv.util.fx.core.view.BaseView
import com.sheryv.util.fx.core.view.ViewFactory
import com.sheryv.util.fx.core.view.ViewState
import com.sheryv.util.logging.log
import javafx.application.Application
import javafx.stage.Stage
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.ModuleDeclaration
import org.koin.logger.SLF4JLogger
import java.lang.management.ManagementFactory
import kotlin.reflect.KClass


abstract class App(protected val primaryViewClass: KClass<out BaseView>, val startupArgs: Array<String>) {
  
  lateinit var primaryStage: Stage
    private set
  lateinit var primaryView: BaseView
    private set
  protected lateinit var koin: Koin
    private set
  
  
  protected open fun launch() {
    Application.launch(FxApplication::class.java, *startupArgs)
  }
  
  protected open fun onInit() {
  
  }
  
  protected open fun onStart() {
    val viewShow = koin.get<ViewFactory>().createWindow(primaryViewClass, stage = primaryStage)
    
    log.debug("App view created at ${ManagementFactory.getRuntimeMXBean().uptime}")
    
    primaryView = viewShow()
  }
  
  protected open fun onReady() {
  
  }
  
  protected open fun onStop() {
  
  }
  
  private fun launchInternal(koin: Koin) {
    this.koin = koin
    launch()
  }
  
  class AppBuilder(val primaryView: KClass<out BaseView>, val startupArgs: Array<String>) {
    private val modules = mutableListOf<Module>()
    private var customAppInstance: () -> App = {
      SimpleApp(primaryView, startupArgs)
    }
    
    val preConfiguration: () -> Unit = {
      System.setProperty("prism.lcdtext", "true")
      System.setProperty("prism.text", "t2k")
      System.setProperty("javafx.autoproxy.disable", "true")
    }
    
    
    fun module(createdAtStart: Boolean = false, moduleDeclaration: ModuleDeclaration) {
      val module = Module(createdAtStart)
      moduleDeclaration(module)
      modules.add(module)
    }
    
    fun customAppInstance(block: () -> App) {
      customAppInstance = block
    }
    
    
    internal fun create(block: AppBuilder.() -> Unit) {
      log.debug("App pre init at ${ManagementFactory.getRuntimeMXBean().uptime}")
      
      preConfiguration()
      
      block()
      val app = customAppInstance()
      val deps = startKoin {
        this.logger(SLF4JLogger())
        modules(org.koin.dsl.module {
          single { app }
          single { AppConfiguration.empty() }
          singleOf(::ViewState)
        })
      }
      
      
      deps.koin.loadModules(
        listOf(
          org.koin.dsl.module {
            single(createdAtStart = true) { deps.koin }
            singleOf(::ViewFactory)
          },
        ) + this.modules
      )
      deps.createEagerInstances()
      
      log.debug("App dependencies created at ${ManagementFactory.getRuntimeMXBean().uptime}")
      
      app.launchInternal(deps.koin)
    }
  }
  
  internal class FxApplication : Application(), KoinComponent {
    
    private val app: App by inject()
    override fun init() {
      app.onInit()
    }
    
    override fun start(primaryStage: Stage) {
      app.primaryStage = primaryStage
      log.debug("App initialized at ${ManagementFactory.getRuntimeMXBean().uptime}")
      
      app.onStart()
      
      log.info("App started at ${ManagementFactory.getRuntimeMXBean().uptime}")
      
      app.onReady()
    }
    
    
    override fun stop() {
      app.onStop()
    }
  }
  
  private class SimpleApp(primaryViewClass: KClass<out BaseView>, startupArgs: Array<String>) : App(primaryViewClass, startupArgs)
  
  companion object {
    @JvmStatic
    inline fun <reified T : BaseView> createAndStart(
      args: Array<String> = arrayOf(),
      noinline block: AppBuilder.() -> Unit
    ) {
      createAndStart(T::class, args, block)
    }
    
    @JvmStatic
    fun createAndStart(
      baseView: KClass<out BaseView>,
      args: Array<String> = arrayOf(),
      block: AppBuilder.() -> Unit
    ) {
      AppBuilder(baseView, args).create(block)
    }
    
  }
}

