package com.sheryv.tools.cloudservermanager.repository

import com.sheryv.tools.cloudservermanager.entities.Authority
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityRepository : JpaRepository<Authority, Long> {
  fun findByCode(code: String): Authority?
  
  override fun delete(user: Authority?)
}
