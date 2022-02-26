package com.sheryv.tools.cloudservermanager.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

enum class Authorities(val code: String) {
  USER_VIEW("USER_VIEW"),
  USER_EDIT("USER_EDIT"),
  MOD_VIEW("MOD_VIEW"),
  MOD_EDIT("MOD_EDIT"),
  ADMIN_EDIT("ADMIN_EDIT")
  ;
  
  fun toAuthority(): GrantedAuthority {
    return SimpleGrantedAuthority(code)
  }
  
  companion object {
    @JvmStatic
    fun userRoles() = arrayOf(USER_VIEW.code, USER_EDIT.code)
    @JvmStatic
    fun moderatorRoles() = arrayOf(MOD_VIEW.code, MOD_EDIT.code) + userRoles()
    @JvmStatic
    fun adminRoles() = arrayOf(ADMIN_EDIT.code) + moderatorRoles()
  }
}


