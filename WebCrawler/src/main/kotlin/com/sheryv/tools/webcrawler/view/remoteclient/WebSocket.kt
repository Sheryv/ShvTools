package com.sheryv.tools.webcrawler.view.remoteclient

import com.sheryv.util.SerialisationUtils
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class WebSocket(val handler: (WsMessage?, String?) -> Unit, val connectionChanged: (Boolean) -> Unit) :
  WebSocketServer(InetSocketAddress("localhost", 45623)) {
  
  private var socket: WebSocket? = null
  
  fun send(msg: WsMessage) {
    socket?.takeIf { it.isOpen }?.send(SerialisationUtils.toJson(msg)) ?: broadcast(SerialisationUtils.toJson(msg))
  }
  
  override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
    if (socket == null && p0 != null) {
      socket = p0
      com.sheryv.util.logging.log.info("Websocket first client connection opened")
    } else {
      com.sheryv.util.logging.log.info("Websocket second client ignored")
    }
    connectionChanged(true)
  }
  
  override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
    com.sheryv.util.logging.log.info("Websocket closed")
    connectionChanged(false)
  }
  
  override fun onMessage(p0: WebSocket?, p1: String?) {
    try {
      val result = SerialisationUtils.fromJson<WsMessage>(p1.orEmpty())
      handler(result, null)
    } catch (e: Exception) {
      com.sheryv.util.logging.log.error("Error parsing websocket message", e)
      handler(null, e.message)
    }
  }
  
  override fun onError(p0: WebSocket?, p1: java.lang.Exception?) {
    com.sheryv.util.logging.log.error("Websocket error", p1)
    connectionChanged(false)
  }
  
  override fun onStart() {
    com.sheryv.util.logging.log.info("Websocket started")
  }
}


data class WsMessage(val type: MsgType, val data: Map<String, Any>, val ref: String = "", val meta: Meta? = null) {
  
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
  
  data class InterceptorMessage(
    val url: String,
    val method: String,
    val headers: List<Header>,
    val body: String? = null,
    val referrer: String? = null
  )
}


