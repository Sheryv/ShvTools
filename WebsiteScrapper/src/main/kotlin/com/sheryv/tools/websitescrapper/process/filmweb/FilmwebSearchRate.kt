package com.sheryv.tools.websitescrapper.process.filmweb

import com.sheryv.tools.websitescrapper.utils.Utils
import java.time.*

class FilmwebSearchRate(
  val favourite: Boolean = false,
  val vote: Int = 0,
  voteDateString: String = "",
  val voteDate: LocalDate? = parseDate(voteDateString),
  val wantToSee: Int = 0
) {
  
  companion object {
    @JvmStatic
    private fun parseDate(search: String): LocalDate? {
      val t = search.trim()
      return when {
        t.contains(' ') -> {
          val parts = t.split(' ').map { it.trim() }.filter { it.isNotBlank() };
          
          val day = parts[0].toInt()
          val month = parseMonth(parts[1]) ?: return null
          val year = if (parts.size > 2) {
            parts[2].toInt()
          } else {
            LocalDate.now().year
          }
          LocalDate.of(year, month, day)
        }
        t == "dzisiaj" -> {
          LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC)
        }
        t == "wczoraj" -> {
          LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC).minusDays(1)
        }
        else -> null
      }
    }
    
    @JvmStatic
    private fun parseMonth(m: String): Month? {
      return when (m) {
        "stycznia", "styczeń" -> Month.JANUARY
        "lutego", "luty" -> Month.FEBRUARY
        "marca", "marzec" -> Month.MARCH
        "kwietnia", "kwiecień" -> Month.APRIL
        "maja", "maj" -> Month.MAY
        "czerwca", "czerwiec" -> Month.JUNE
        "lipca", "lipiec" -> Month.JULY
        "sierpnia", "sierpień" -> Month.SEPTEMBER
        "września", "wrzesień" -> Month.AUGUST
        "października", "październik" -> Month.OCTOBER
        "listopada", "listopad" -> Month.NOVEMBER
        "grudnia", "grudzień" -> Month.DECEMBER
        else -> null
      }
    }
  }
}
