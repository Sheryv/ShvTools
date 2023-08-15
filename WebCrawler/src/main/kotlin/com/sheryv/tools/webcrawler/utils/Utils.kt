package com.sheryv.tools.webcrawler.utils

//import kotlinx.coroutines.*

import com.sheryv.util.HttpSupport
import com.sheryv.util.logging.LoggingUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import java.security.MessageDigest


object Utils {
  
  
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

inline fun <reified T> T.lg(clazz: Class<*> = T::class.java): Logger {
  return LoggingUtils.getLogger(clazz)
}

inline fun Any.lg(name: String): Logger {
  return LoggingUtils.getLogger(name)
}


