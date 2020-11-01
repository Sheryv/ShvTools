package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.utils.DataUtils
import org.junit.jupiter.api.Test
import java.io.File

class DataTest {
  @Test
  fun name() {
    val map = DataUtils.downloadAndParse("https://jsonplaceholder.typicode.com/todos/1", Map::class.java)
    val map2 = DataUtils.downloadAndParse("https://github.com/eugenp/tutorials/raw/master/spring-boot-modules/spring-boot-properties/src/main/resources/database.yml", Map::class.java)
    println()
  }
  
  @Test
  fun download() {
    DataUtils.downloadFile("https://edge.forgecdn.net/files/2902/483/TConstruct-1.12.2-2.13.0.183.jar", File("C:temp\\tc-test.jar"))
//    DataUtils.downloadFile("https://media.forgecdn.net/files/2902/483/TConstruct-1.12.2-2.13.0.183.jar", File("C:temp\\tc-test.jar"))
  }
}