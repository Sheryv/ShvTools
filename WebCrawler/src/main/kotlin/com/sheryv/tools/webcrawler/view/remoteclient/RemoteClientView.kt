package com.sheryv.tools.webcrawler.view.remoteclient

import com.fasterxml.jackson.module.kotlin.convertValue
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.utils.DialogUtils
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.Strings
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.*
import com.sheryv.util.logging.log
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.Priority
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class HttpServerView(override val config: Configuration) : SimpleView() {
  private val input = stringProperty("")
  private val filePath = stringProperty(config.remoteClient.scriptPath.toAbsolutePath().toString())
  private val output = stringProperty("")
  private var server: WS? = null
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
              DialogUtils.openFileDialog(stage, initialFile = filePath.value)?.also { filePath.set(it.toAbsolutePath().toString()) }
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
    
    val msg = Message(MsgType.EXECUTE_SCRIPT, mapOf("code" to script.trim()), Strings.generateId(6))
    server!!.send(msg)
  }
  
  override fun onViewReady() {
    server = WS({ m, e ->
      val line = if (m != null) {
        "[M] $m"
      } else {
        "[E] $e"
      }
      
      output.value += "\n" + line
      log.debug(line)
      if (m?.type == MsgType.INTERCEPT) {
        log.info("intercept {}", SerialisationUtils.jsonMapper.convertValue<Intercepted>(m.data))
      }
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

class WS(val handler: (Message?, String?) -> Unit, val connectionChanged: (Boolean) -> Unit) :
  WebSocketServer(InetSocketAddress("localhost", 45623)) {
  
  private var socket: WebSocket? = null
  
  fun send(msg: Message) {
    socket?.takeIf { it.isOpen }?.send(SerialisationUtils.toJson(msg)) ?: broadcast(SerialisationUtils.toJson(msg))
  }
  
  override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
    if (socket == null && p0 != null) {
      socket = p0
      log.info("Websocket first client connection opened")
    } else {
      log.info("Websocket second client ignored")
    }
    connectionChanged(true)
  }
  
  override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
    log.info("Websocket closed")
    connectionChanged(false)
  }
  
  override fun onMessage(p0: WebSocket?, p1: String?) {
    try {
      val result = SerialisationUtils.fromJson<Message>(p1.orEmpty())
      handler(result, null)
    } catch (e: Exception) {
      log.error("Error parsing websocket message", e)
      handler(null, e.message)
    }
  }
  
  override fun onError(p0: WebSocket?, p1: java.lang.Exception?) {
    log.error("Websocket error", p1)
    connectionChanged(false)
  }
  
  override fun onStart() {
    log.info("Websocket started")
  }
}


data class Message(val type: MsgType, val data: Map<String, Any>, val ref: String = "", val meta: Meta? = null) {


}

data class Meta(val website: String, val data: String)

enum class MsgType {
  EXECUTE_SCRIPT,
  META,
  LOG,
  INTERCEPT,
  RESULTS,
}

data class LogBody(val level: String, val msg: String, val args: Map<String, String>)

data class Header(val name: String, val value: String)

data class Intercepted(
  val url: String,
  val method: String,
  val headers: List<Header>,
  val body: String? = null,
  val referrer: String? = null
)
