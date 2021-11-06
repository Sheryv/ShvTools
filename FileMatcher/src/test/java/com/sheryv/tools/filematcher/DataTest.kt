package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.utils.DataUtils
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Test
import java.io.File

class DataTest {
  @Test
  fun name() {
    val map = DataUtils.downloadAndParse("https://jsonplaceholder.typicode.com/todos/1", Map::class.java)
    val map2 = DataUtils.downloadAndParse(
      "https://github.com/eugenp/tutorials/raw/master/spring-boot-modules/spring-boot-properties/src/main/resources/database.yml",
      Map::class.java
    )
    println()
  }
  
  @Test
  fun urlTransform() {
    println(StringUtils.replaceEach("abcde", arrayOf("c", "e"), arrayOf("_")))
    
    val s = "https://www.curseforge.com/minecraft/mc-mods/better-shields/download/3183362/file"
    val t = "https://edge.forgecdn.net/files/"
    val file = "BetterShieldsMC1.16.3-1.1.4.jar"
    var r = ""
    val regex = Regex("curseforge\\.com/minecraft.+download/(\\d{5,})")
    val find = regex.find(s)
    if (find != null) {
      val part = find.groupValues[1]
      r = t + part.take(4) + "/" + part.substring(4) + "/" + file
    }
    println(r)
  }
  
  @Test
  fun download() {
    DataUtils.downloadFile(
      "https://media.forgecdn.net/files/2491/32/ae2stuff-0.7.0.4-mc1.12.2.jar",
      File("C:temp\\tc-test.jar")
    )
//    DataUtils.downloadFile("https://media.forgecdn.net/files/2902/483/TConstruct-1.12.2-2.13.0.183.jar", File("C:temp\\tc-test.jar"))
  }
  
  @Test
  fun files() {
    val dir = File("D:\\ShvFileMatcher\\target\\mods")
    
    val regex = Regex("([+A-Za-z\\d_-]+(\\d+)?)[_-]([\\w.]+[_-])?(.*)\\.jar")
    val regex2 = Regex("([+A-Za-z\\d_-]+)([_-]\\w*\\d+)?\\.jar")
    val check = Regex("\\d\\.\\d")
    val res = dir.listFiles()!!.map {
      val matchResult = if (check.find(it.name) != null) {
        regex.matchEntire(it.name)
      } else {
        println("check2 ${it.name}")
        regex2.matchEntire(it.name)
      }
      
      if (matchResult != null) {
        val prefix = matchResult.groupValues[1].padEnd(30)
        val vGroup = if (matchResult.groupValues.size > 4) {
          4
        } else {
          2
        }
        val version = matchResult.groupValues[vGroup].padEnd(15)
        return@map "$prefix | $version > ${it.name}"
      }
      
      
      "===============\n>>>>> ${it.name}\n================"
    }
    
    res.forEach { println(it) }
    println()
  }
  
  
}
