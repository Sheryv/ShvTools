package com.sheryv.tools.cloudservermanager.entities

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import javax.persistence.*

@Entity
@Table(name = "authorities")
class Authority(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  val code: String? = null,
) {
  fun toAuthority(): GrantedAuthority {
    return SimpleGrantedAuthority(code)
  }
}
