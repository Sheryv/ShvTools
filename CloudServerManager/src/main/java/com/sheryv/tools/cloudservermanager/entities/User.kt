package com.sheryv.tools.cloudservermanager.entities

import com.sheryv.tools.cloudservermanager.model.Authorities
import javax.persistence.*
import kotlin.jvm.Transient

@Entity
@Table(name = "users")
class User {
  @Id
  @Column(unique = true, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null
  
  @Column(length = 40)
  var username: String? = null
  
  @Column(length = 60)
  var password: String? = null
  
  @Column(name = "enabled")
  var isEnabled = true
  
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "users_authorities",
    joinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "authority_id", referencedColumnName = "id")]
  )
  public var roles: Collection<Authority> = emptyList()
  
  @Transient
  val isAdmin = roles.any { it.code == Authorities.ADMIN_EDIT.code }
  

}
