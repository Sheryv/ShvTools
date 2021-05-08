package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

private val REGEXES = mutableMapOf<String, Regex>()
private val WILDCARDS_MAPPING = mapOf('*' to ".*", '?' to ".")

data class Matching(
  val regex: String? = null,
  val wildcard: String? = null,
  val prefix: String? = null,
  val options: MatchingOptions? = null,
) {
  //  @JsonIgnore
//  val strategy: MatchingStrategy = if (pattern.isNullOrEmpty()) MatchingStrategy.EXACT else MatchingStrategy.REGEX
//
  @JsonIgnore
  var lastMatches: List<File> = emptyList()
  
  @JsonIgnore
  public fun isConfigured(): Boolean {
    return regex != null || wildcard != null || prefix != null;
  }
  
  @JsonIgnore
  public fun getPattern() = regex ?: wildcard ?: prefix

  public fun matches(text: String): Boolean {
    if (regex != null) {
      validate(regex)
      val r = REGEXES.computeIfAbsent(regex) { Regex(regex) }
      if (options?.regexMatchWholeName != false) {
        return r.matchEntire(text) != null
      } else {
        return r.find(text) != null
      }
    } else if (wildcard != null) {
      validate(wildcard)
      val r = REGEXES.computeIfAbsent(wildcard) { wildcardToRegex(wildcard) }
      return r.matchEntire(text) != null
    } else if (prefix != null) {
      validate(prefix)
      return text.startsWith(prefix)
    } else {
      throw IllegalStateException("Matching is not configured (while trying to match '$text')")
    }
  }
  
  private fun validate(pattern: String) {
    if (pattern.contains('/')) {
      throw IllegalArgumentException("Character '/' is not allowed in pattern field for file name matcher. Value: '${pattern}'")
    }
  }
  
  private fun wildcardToRegex(input: String): Regex {
    var start = 0
    val sb = StringBuilder()
    for (c in input.indices) {
      if (WILDCARDS_MAPPING.containsKey(input[c])) {
        val part = input.substring(start, c)
        sb.append(Regex.escape(part)).append(WILDCARDS_MAPPING[input[c]])
        start = c + 1
      }
    }
    val part = input.substring(start)
    if (part.isNotBlank()) {
      sb.append(Regex.escape(part))
    }
    return Regex(sb.toString())
  }
  
  data class MatchingOptions(val regexMatchWholeName: Boolean = true)
}
