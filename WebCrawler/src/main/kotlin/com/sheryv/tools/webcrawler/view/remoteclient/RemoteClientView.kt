package com.sheryv.tools.webcrawler.view.remoteclient

import com.fasterxml.jackson.module.kotlin.convertValue
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.view.remoteclient.WsMessage.MsgType
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.Strings
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.*
import com.sheryv.util.logging.log
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.Priority
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class HttpServerView(override val config: Configuration) : SimpleView() {
  private val input = stringProperty("")
  private val filePath = stringProperty(config.remoteClient.scriptPath.toAbsolutePath().toString())
  private val output = stringProperty("")
  private var server: com.sheryv.tools.webcrawler.view.remoteclient.WebSocket? = null
  private val connected = booleanProperty(false)
  private val inProgress = booleanProperty(false)
  
  override val root: Parent by lazy {
    title = "HttpServer test"
    createRoot {
      paddingAll = 5.0
      vbox {
        alignment = Pos.CENTER_LEFT
        isFillWidth = true
        vgrow = Priority.ALWAYS
        spacing = 10.0
        hbox {
          textfield(filePath) {
            hgrow = Priority.ALWAYS
          }
          button("...") {
            setOnAction {
              factory.dialogs.openFileDialog(stage, initialFile = filePath.value)?.also { filePath.set(it.toAbsolutePath().toString()) }
            }
          }
          button("Execute from file") {
            styleClass.add("btn-success")
            disableProperty().bind(connected.or(inProgress))
            setOnAction {
              try {
                val p = filePath.value?.let { Path.of(it) }
                if (p?.exists() == true) {
                  inProgress.set(true)
                  execute(Files.readString(p))
                } else {
                  log.warn("File '$p' does not exist ")
                }
              } catch (ex: Exception) {
                log.error("Cannot execute", ex)
              } finally {
                inProgress.set(false)
              }
            }
          }
          button("Execute below script") {
            styleClass.add("btn-success")
            disableProperty().bind(connected)
            setOnAction {
              try {
                isDisable = true
                
                execute(
                  """function run(shv) {
                    ${input.value}
                  }""".trimIndent()
                )
              } finally {
                isDisable = false
              }
            }
          }
        }
        splitpane(Orientation.VERTICAL) {
          vgrow = Priority.ALWAYS
          dividerPositions
          vbox {
            label("Input") {
              paddingTop = 5.0
              paddingLeft = 5.0
            }
            textarea(input) {
              vgrow = Priority.ALWAYS
              styleClass.add("mono")
              
            }
          }
          vbox {
            label("Output") {
              paddingTop = 5.0
              paddingLeft = 5.0
            }
            textarea(output) {
              vgrow = Priority.ALWAYS
              isEditable = false
              styleClass.add("mono")
            }
          }
        }
        label("Remote client should connect on ws://localhost:45623")
      }
    }
  }
  
  private fun execute(script: String) {
    if (!script.contains("function run(shv) {") && !script.contains("function run(shv){")) {
      throw RuntimeException("Script have to contain top level run function with signature:\nfunction run(shv)")
    }
    
    val msg = WsMessage(MsgType.EXECUTE_SCRIPT, mapOf("code" to script.trim()), Strings.generateId(6))
    server!!.send(msg)
  }
  
  override fun onViewReady() {
    server = WebSocket({ m, e ->
      val line = if (m != null) {
        "[M] $m"
      } else {
        "[E] $e"
      }
      
      output.value += "\n" + line
      log.debug(line)
    }, { connected.set(!it) }
    ).also {
      it.connectionLostTimeout = 10
      it.start()
    }
    filePath.onChangeNotNull {
      config.remoteClient.scriptPath = Path.of(it)
      config.save()
    }
    super.onViewReady()
  }
  
  override fun onViewDestroy() {
    super.onViewDestroy()
    server?.stop()
  }

//  fun startServer() {
//    val server = HttpServer.create(InetSocketAddress(45623), 0)
//    server.createContext("/test", MyHandler())
//    server.executor = null // creates a default executor
//    server.start()
//  }
//
//  class MyHandler : HttpHandler {
//    override fun handle(t: HttpExchange) {
//      val response = "This is the response"
//      t.sendResponseHeaders(200, response.length.toLong())
//      val os = t.responseBody
//      os.write(response.toByteArray())
//      os.close()
//    }
//  }
}


