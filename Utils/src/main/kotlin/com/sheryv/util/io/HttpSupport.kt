package com.sheryv.util.io

import com.sheryv.util.logging.LoggingUtils.getLogger
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URISyntaxException
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedTrustManager

class HttpSupport(client: HttpClient = HttpClient.newHttpClient(), securityEnabled: Boolean = true) {
  private val client: HttpClient
  
  init {
    if (securityEnabled) {
      this.client = client
    } else {
      try {
        val sslContext = unsecureSslContext()
        this.client = HttpClient.newBuilder().sslContext(sslContext).build()
      } catch (e: Exception) {
        throw RuntimeException(e)
      }
    }
  }
  
  fun sendGet(url: String?): HttpResponse<String> {
    val request = getRequest(url)
    return send(request)
  }
  
  fun sendString(url: String): String {
    val request = getRequest(url)
    return send(request).body()
  }
  
  fun sendPost(url: String, body: String): HttpResponse<String> {
    return try {
      val request = HttpRequest.newBuilder(URL(url).toURI()).POST(HttpRequest.BodyPublishers.ofString(body)).build()
      send(request)
    } catch (e: URISyntaxException) {
      throw RuntimeException(e)
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }
  
  fun send(request: HttpRequest): HttpResponse<String> {
    return try {
      client.send(request, HttpResponse.BodyHandlers.ofString())
    } catch (e: IOException) {
      getLogger(javaClass).debug("Error executing request to %s".format(request.uri()), e)
      throw RuntimeException(e)
    } catch (e: InterruptedException) {
      getLogger(javaClass).debug("Error executing request to %s".format(request.uri()), e)
      throw RuntimeException(e)
    }
  }
  
  fun stream(request: HttpRequest): HttpResponse<InputStream> {
    return try {
      client.send(request, HttpResponse.BodyHandlers.ofInputStream())
    } catch (e: IOException) {
      throw RuntimeException(e)
    } catch (e: InterruptedException) {
      throw RuntimeException(e)
    }
  }
  
  //  fun streamWithProgress(request: HttpRequest, bufferSize: Int = 1024 * 16): Sequence<ByteArray> {
//
//    return sequence {
//      stream(request).use { inputStream ->
//
//
////        val readableByteChannel: ReadableByteChannel = Channels.newChannel(inputStream)
////        val channel: FileChannel = fileOutputStream.getChannel()
//        var buffer = ByteArray(bufferSize)
//        var read = inputStream.read(buffer)
//        while (read > 0) {
//
//
//          if (read < buffer.size) {
//            yield(buffer.copyInto(ByteArray(read), 0, 0, read))
//          } else {
//            yield(buffer)
//          }
////          val speed = read / ((System.currentTimeMillis() - time).toDouble() / 1000.0)
////          if (speed >= 0) {
////            progress!!.currentSpeed = speed
////          }
//
////          if (headerSize > 0) {
////            progress!!.currentRatio = progress!!.currentBytes.toDouble() / headerSize
////          }
////          progress!!.increaseBytes(read)
////          time = System.currentTimeMillis()
////          read = channel.transferFrom(readableByteChannel, progress!!.currentBytes, BUFFER)
//          for (i in 0..buffer.size) {
//            buffer[i] = 0
//          }
//          read = inputStream.read(buffer)
//        }
////        if (headerSize != progress!!.currentBytes) {
////          System.out.printf("Sizes are different: %d, %d%n", progress!!.currentBytes, headerSize)
////        }
////        if (progress!!.currentBytes < 10) {
////          progress!!.currentRatio = 0.0
////          throw IllegalStateException("Empty stream returned")
////        }
////        isSuccessful = true
////        progress!!.currentRatio = 1.0
////        progress!!.finishTime = Instant.now()
////        return progress!!.currentBytes
//      }
//    }
//  }
//
  fun streamWithProgress(request: HttpRequest): HttpResponseStreamTask {
    return HttpResponseStreamTask(stream(request))
  }
  
  fun downloadWithProgress(
    request: HttpRequest,
    output: Path,
    bufferSize: Int = 1024 * 64,
    onProgress: suspend (DataTransferProgress) -> Unit
  ): DataTransferProgressSummary {
    return streamWithProgress(request).toFile(output, bufferSize, onProgress)
  }

//    fun downloadWithProgress(
//    request: HttpRequest,
//    output: Path,
//    bufferSize: Long = 1024 * 64,
//    onProgress: suspend (ProgressInputStream.ProgressPart) -> Unit
//  ) {
//    streamWithProgress(request, onProgress).use { inputStream ->
//      val readableByteChannel: ReadableByteChannel = Channels.newChannel(inputStream)
//
//      FileOutputStream(output.toFile()).use {
//        val channel: FileChannel = it.getChannel()
//        var read: Long = channel.transferFrom(readableByteChannel, 0, bufferSize)
//        var transferred = read
//        while (read > 0) {
//          read = channel.transferFrom(readableByteChannel, transferred, bufferSize)
//          transferred += read
//        }
//      }
//    }
//  }
//
  
  fun getContentSize(url: String): Long? {
    var urlConnection: HttpURLConnection? = null
    try {
      urlConnection = (URL(url).openConnection() as HttpURLConnection)
      urlConnection.connect()
      return urlConnection.contentLengthLong.takeIf { it > 0 }
    } finally {
      urlConnection?.disconnect()
    }
  }
  
  companion object {
    @JvmStatic
    fun unsecureSslContext(): SSLContext {
      val trustAllCerts = arrayOf<TrustManager>(object : X509ExtendedTrustManager() {
        override fun getAcceptedIssuers(): Array<X509Certificate>? {
          return null
        }
        
        override fun checkClientTrusted(
          a_certificates: Array<X509Certificate>,
          a_auth_type: String
        ) {
        }
        
        override fun checkServerTrusted(
          a_certificates: Array<X509Certificate>,
          a_auth_type: String
        ) {
        }
        
        override fun checkClientTrusted(
          a_certificates: Array<X509Certificate>,
          a_auth_type: String,
          a_socket: Socket
        ) {
        }
        
        override fun checkServerTrusted(
          a_certificates: Array<X509Certificate>,
          a_auth_type: String,
          a_socket: Socket
        ) {
        }
        
        override fun checkClientTrusted(
          a_certificates: Array<X509Certificate>,
          a_auth_type: String,
          a_engine: SSLEngine
        ) {
        }
        
        override fun checkServerTrusted(
          a_certificates: Array<X509Certificate>,
          a_auth_type: String,
          a_engine: SSLEngine
        ) {
        }
      })
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(null, trustAllCerts, SecureRandom())
      return sslContext
    }
    
    @JvmStatic
    fun getRequest(url: String?): HttpRequest {
      return try {
        HttpRequest.newBuilder(URL(url).toURI()).GET().build()
      } catch (e: URISyntaxException) {
        throw RuntimeException(e)
      } catch (e: IOException) {
        throw RuntimeException(e)
      }
    }
  }
}
