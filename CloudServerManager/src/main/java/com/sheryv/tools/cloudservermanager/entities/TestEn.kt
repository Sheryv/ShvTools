package com.sheryv.tools.cloudservermanager.entities

import com.querydsl.core.annotations.QueryEntity
import com.sheryv.tools.cloudservermanager.model.Authorities
import java.time.OffsetDateTime
import java.util.*

@QueryEntity
class TestEn {
  val inn: Int? = null
  val str: String? = null
  val dd: OffsetDateTime? = null
  val bb: Boolean? = null
  val d2: Date? = null
  val xczx: Long? = null
  val nvbdf: List<String> = emptyList()
  val eee: Authorities? = null
}
