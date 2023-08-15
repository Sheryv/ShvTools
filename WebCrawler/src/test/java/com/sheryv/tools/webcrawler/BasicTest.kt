package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.utils.Utils
import org.apache.http.client.methods.RequestBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.*


class BasicTest {
  val bases = listOf(
    Path.of("H:\\Movies"),
  )
  
  @Test
  @Disabled
  fun fixTvShow() {
    for (base in bases) {
      fixTvShowsFileStructure(base)
      generateNfo(base)
    }
  }
  
  private fun fixTvShowsFileStructure(base: Path = bases.first()) {
    val patternEpisode = Regex("""(.*) [sS](\d\d)[eE](\d\d)""")
    val patternSeries = Regex("""(.+) (\d\d)""")
    
    for (show in base.listDirectoryEntries().filter { it.isDirectory() && patternSeries.containsMatchIn(it.name) }) {
      val (_, title, season) = patternSeries.find(show.name)!!.groupValues
      
      val files = show.listDirectoryEntries().filter { it.isRegularFile() && patternEpisode.containsMatchIn(it.name) }
      if (files.isNotEmpty()) {
        val seasonDir = base.resolve(title).resolve(String.format("Season %02d", season.toInt()))
        Files.createDirectories(seasonDir)
        
        for (file in files) {
          
          val target = seasonDir.resolve(patternEpisode.replace(file.name, " S$2E$3").trim())
          if (target.exists()) {
            println("File already exists " + target.toAbsolutePath())
          } else {
//            println("Move $file -> $target")
            Files.move(file, target, StandardCopyOption.ATOMIC_MOVE)
          }
        }
      }
    }
  }
  
  private fun generateNfo(base: Path = bases.first()) {
    val patternEpisode = Regex("""(.*) ?[sS](\d\d)[eE](\d\d) ?(- (.*))?\.(ts|mp4|mkv|m4v)+""")
    val patternSeries = Regex("""(.+) (\d\d)""")
    
    for (oldShow in base.listDirectoryEntries().filter { it.isDirectory() && patternSeries.containsMatchIn(it.name) }) {
      val show = base.resolve(patternSeries.find(oldShow.name)!!.groupValues[1])
      
      if (!show.exists()) {
        println("Skipped $oldShow")
        continue
      }
      
      for (season in show.listDirectoryEntries().filter { it.isDirectory() && it.name.startsWith("Season") }) {
        val files = season.listDirectoryEntries().filter { it.isRegularFile() && patternEpisode.containsMatchIn(it.name) }
        for (file in files) {
          
          val groupValues = patternEpisode.find(file.name)!!.groupValues
          val target = if (groupValues[1].isBlank()) {
            val fixed = Files.move(file, file.resolveSibling(show.name + " " + file.name.trim()))
            
            season.resolve(fixed.nameWithoutExtension + ".nfo")
          } else {
            season.resolve(file.nameWithoutExtension + ".nfo")
          }
          val seasonNumber = groupValues[2]
          val number = groupValues[3]
          val title = groupValues[5].ifBlank { "Episode $number" }
          
          
          val episodeNfo = """
              <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
              <episodedetails>
                <title>${title}</title>
                <originaltitle/>
                <showtitle>${show.name}</showtitle>
                <season>${seasonNumber.toInt()}</season>
                <episode>${number.toInt()}</episode>
                <displayseason>-1</displayseason>
                <displayepisode>-1</displayepisode>
                <id/>
                <ratings/>
                <userrating>0.0</userrating>
                <plot/>
                <mpaa/>
                <premiered/>
                <aired/>
                <watched>false</watched>
                <playcount>0</playcount>
                <trailer/>
                <dateadded/>
                <epbookmark/>
                <code/>
              </episodedetails>
            """.trimIndent()
          
          Files.writeString(target, episodeNfo, StandardOpenOption.CREATE)
        }
      }
      
      val seriesNfo = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <tvshow>
          <title>${show.name}</title>
          <originaltitle/>
          <showtitle>${show.name}</showtitle>
          <sorttitle/>
          <year/>
          <ratings/>
          <userrating>0.0</userrating>
          <outline/>
          <plot/>
          <tagline/>
          <runtime>0</runtime>
          <mpaa/>
          <certification/>
          <id/>
          <imdbid/>
          <tmdbid/>
          <premiered/>
          <status>Unknown</status>
          <watched>false</watched>
          <playcount/>
          <studio/>
          <country/>
          <trailer/>
          <dateadded/>
        </tvshow>
        """.trimIndent()
      val target = show.resolve("tvshow.nfo")
      Files.writeString(target, seriesNfo, StandardOpenOption.CREATE)
      
      if (oldShow.listDirectoryEntries().isEmpty()) {
        Files.deleteIfExists(oldShow)
      } else {
        println("Old dir is not empty $oldShow")
      }
    }
  }
  
  
  @Test
  fun name() {
    val s = Utils.httpClientExecute(
      RequestBuilder.get("https://delivery-node-3qxdghox0csibqlw.voe-network.net/engine/hls2/01/08895/svjgkz8xnctq_n/master.m3u8?t=3-nHgQRP62mARo0N3VTG2t-U5LQEiu0IhA6oJ8qlrBc&s=1682276985&e=14400&f=44475871&node=delivery-node-3qxdghox0csibqlw.voe-network.net&i=77.65&sp=4500&asn=212163")
        .build()
    )
    println(">>> ${s}")
  }
}

