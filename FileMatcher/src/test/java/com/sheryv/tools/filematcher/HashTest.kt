package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.timeLog
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Paths

class HashTest {
  
  @Test
  fun name() {
    val path = Paths.get("d:XenyPackUpdaterData\\Shiginima Launcher SE v4.100.jar")
    println("MD5:     " + timeLog("md5") {
      Hashing.md5(path)
    })
    println("SHA-1:   " + timeLog("SHA-1") {
      Hashing.sha1(path)
    })
    println("SHA-256: " + timeLog("SHA-256") {
      Hashing.sha256(path)
    })
    println("CRC-32:  " + timeLog("CRC-32") {
      Hashing.crc32(path)
    })
    println("BLAKE2s: " + timeLog("BLAKE2s") {
      Hashing.blake2s(path)
    })
  }
  
  @Test
  fun download() {
    val uri = URI.create("file://d/XenyPackUpdaterData/Shiginima%20Launcher%20SE%20v4.100.jar")
    
    val s = uri.toURL()
    println()
  }
}