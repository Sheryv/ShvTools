package com.sheryv.tools.webcrawler.utils

//import kotlinx.coroutines.*

import com.sheryv.util.io.HttpSupport
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.security.MessageDigest


object Utils {
  
  fun getChunksOfM3U8Video(url: String): List<String> {
    val baseURL = url.substring(0, url.lastIndexOf("/") + 1)
    return HttpSupport().sendString(url).lines()
      .filter { it.contains(".ts") && !it.trimStart().startsWith("#") }
      .map {
        if (it.startsWith("http:") || it.startsWith("https:")) {
          it
        } else {
          baseURL + it
        }
      }
  }
  
  fun md5Hash(text: String): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    md.update(text.encodeToByteArray())
    return md.digest()
  }
  
  fun httpClientExecute(request: HttpUriRequest): String {
    val sslsf = SSLConnectionSocketFactory(
      HttpSupport.unsecureSslContext(),
      arrayOf("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"),
      null,
      NoopHostnameVerifier.INSTANCE
    )
    
    val client = HttpClients.custom()
      .setSSLSocketFactory(sslsf)
      .build()
    
    return client.use { httpclient ->
      httpclient.execute(request) { response: HttpResponse ->
        EntityUtils.toString(response.entity)
      }
    }
  }
}
