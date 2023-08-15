package com.sheryv.util

import com.sheryv.util.logging.LoggingUtils.getLogger
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.net.URISyntaxException
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
  
  fun sendString(url: String?): String {
    val request = getRequest(url)
    return send(request).body()
  }
  
  fun sendPost(url: String?, body: String?): HttpResponse<String> {
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
  
  fun stream(request: HttpRequest?): InputStream {
    return try {
      client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()
    } catch (e: IOException) {
      throw RuntimeException(e)
    } catch (e: InterruptedException) {
      throw RuntimeException(e)
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
