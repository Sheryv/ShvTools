package com.sheryv.tools.cmd.convertmovienames.videosearch

import com.fasterxml.jackson.module.kotlin.readValue
import com.sheryv.tools.cmd.convertmovienames.ConvertMovieNames
import com.sheryv.tools.cmd.convertmovienames.VERBOSE
import com.sheryv.util.HttpSupport
import java.net.URLEncoder
import java.net.http.HttpResponse

object TmdbApi {
  
  fun searchTv(search: String): List<TvSearchItem> {
    val query = URLEncoder.encode(search)
    val request = "https://api.themoviedb.org/3/search/tv?api_key=${ConvertMovieNames.config.tmdbKey}&language=en-US&query=$query&page=1"
    
    val response = send(request)
    
    val result: SearchResult<TvSearchItem> = ConvertMovieNames.mapper.readValue(response.body())
    val items = result.results.sortedByDescending { it.popularity }
    println("Found ${items.size} items: ${items.joinToString("\n", "\n", "\n") { it.toString() }}")
    return items
  }
  
  fun searchMovie(search: String): List<MovieSearchItem> {
    val query = URLEncoder.encode(search)
    val request = "https://api.themoviedb.org/3/search/movie?api_key=${ConvertMovieNames.config.tmdbKey}&language=en-US&query=$query&page=1"
    
    val response = send(request)
    
    val result: SearchResult<MovieSearchItem> = ConvertMovieNames.mapper.readValue(response.body())
    val items = result.results.sortedByDescending { it.popularity }
    println("Found ${items.size} items: ${items.joinToString("\n", "\n", "\n") { it.toString() }}")
    return items
  }
  
  private fun send(request: String): HttpResponse<String> {
    val support = HttpSupport()
    
    return try {
      val r = support.sendGet(request)
      if (VERBOSE) {
        println(">>> ${r.statusCode()} $request\n${r.body()}")
      }
      r
    } catch (e: Exception) {
      println("Error executing request: $request")
      throw e
    }
  }
}
