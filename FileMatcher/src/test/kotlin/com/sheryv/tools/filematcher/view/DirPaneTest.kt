package com.sheryv.tools.filematcher.view

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Files.exists
import java.nio.file.Files.readString
import java.nio.file.Path

class DirPaneTest {
  @Test
  fun readJar() {
    FileSystems.newFileSystem(Path.of("D:\\ShvFileMatcher\\out\\mods\\architectury-9.2.14-forge.jar")).use { fileSystem ->
      (fileSystem.getPath("META-INF/mods.toml").takeIf(Files::exists) ?: fileSystem.getPath("META-INF/neoforge.mods.toml").takeIf(Files::exists))?.let(Files::readString)
    }?.let {
      val r = Regex("""modId *= *"(\S+)"""")
      r.find(it)?.let { matchResult ->
        val modId = matchResult.groupValues[1]
        println("modId: $modId")
      }
    }
  }
}
