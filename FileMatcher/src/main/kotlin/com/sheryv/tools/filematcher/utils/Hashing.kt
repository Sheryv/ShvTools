package com.sheryv.tools.filematcher.utils

import org.kocakosm.jblake2.Blake2s
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.CRC32
import kotlin.system.measureNanoTime


object Hashing {
  fun sha256(path: Path): String {
    return buildInHash("SHA-256", path)
  }
  
  fun sha1(path: Path): String {
    return buildInHash("SHA-1", path)
  }
  
  fun md5(path: Path): String {
    return buildInHash("MD5", path)
  }
  
  fun crc32(path: Path): String {
    val inputStream = BufferedInputStream(Files.newInputStream(path))
    val crc = CRC32()
    var cnt: Int = inputStream.read()
    while (cnt != -1) {
      crc.update(cnt)
      cnt = inputStream.read()
    }
    
    return java.lang.Long.toHexString(crc.value).uppercase()
  }
  
  
  // D28A55FD3B9124C56CB81233BBD43B2CD69A3BC9F6661F0720370EDEC72E5AD4
//  ED5F367C2289ACB51EEC2928D9CADA6719D6807F3522B3F553D0D21D451168D7
  fun blake2s(path: Path): String {
    val result = BufferedInputStream(FileInputStream(path.toFile())).use { bis ->
      val blake2s = Blake2s(32)
      val buf = ByteArray(4096)
      var read = bis.read(buf)
      while (read != -1) {
        blake2s.update(buf, 0, read)
        read = bis.read(buf)
      }
      blake2s.digest()
    }
    return toHex(result)
  }
  
  fun <T> time(name: String, source: Class<*>, repeats: Int = 1, b: () -> T): T {
    var res: T? = null
    val time = measureNanoTime {
      for (i in 0..repeats) {
        
        if (i == 0) {
          res = b()
        } else {
          b()
        }
        
      }
    } / repeats / 1_000_000.0
    LoggerFactory.getLogger(source).info("Time calc - avg from $repeats tries: ${name.padEnd(25)} -> " + time)
    return res!!
  }
  
  private fun buildInHash(algorithm: String, path: Path): String {
    val md: MessageDigest = MessageDigest.getInstance(algorithm)
    
    val result = BufferedInputStream(FileInputStream(path.toFile())).use { bis ->
      val buf = ByteArray(4096)
      var read: Int
      read = bis.read(buf)
      
      while (read != -1) {
        md.update(buf, 0, read)
        read = bis.read(buf)
      }
      md.digest()
    }
    return toHex(result)
  }
  
  
  private val LOOKUP_TABLE_LOWER = charArrayOf(0x30.toChar(), 0x31.toChar(), 0x32.toChar(), 0x33.toChar(), 0x34.toChar(), 0x35.toChar(), 0x36.toChar(), 0x37.toChar(), 0x38.toChar(), 0x39.toChar(), 0x61.toChar(), 0x62.toChar(), 0x63.toChar(), 0x64.toChar(), 0x65.toChar(), 0x66.toChar())
  private val LOOKUP_TABLE_UPPER = charArrayOf(0x30.toChar(), 0x31.toChar(), 0x32.toChar(), 0x33.toChar(), 0x34.toChar(), 0x35.toChar(), 0x36.toChar(), 0x37.toChar(), 0x38.toChar(), 0x39.toChar(), 0x41.toChar(), 0x42.toChar(), 0x43.toChar(), 0x44.toChar(), 0x45.toChar(), 0x46.toChar())
  
  fun toHex(byteArray: ByteArray, upperCase: Boolean = true, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): String {
    
    // our output size will be exactly 2x byte-array length
    val buffer = CharArray(byteArray.size * 2)
    
    // choose lower or uppercase lookup table
    val lookup = if (upperCase) LOOKUP_TABLE_UPPER else LOOKUP_TABLE_LOWER
    var index: Int
    for (i in byteArray.indices) {
      // for little endian we count from last to first
      index = if (byteOrder == ByteOrder.BIG_ENDIAN) i else byteArray.size - i - 1
      
      // extract the upper 4 bit and look up char (0-A)
      buffer[i shl 1] = lookup[byteArray[index].toInt() shr 4 and 0xF]
      // extract the lower 4 bit and look up char (0-A)
      buffer[(i shl 1) + 1] = lookup[byteArray[index].toInt() and 0xF]
    }
    return String(buffer)
  }
}
