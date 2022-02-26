package com.sheryv.tools.cloudservermanager.repository

import com.sheryv.tools.cloudservermanager.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {
  @Query("select u from User u left join fetch u.roles where u.username = ?1")
  fun findByUsername(username: String?): User?
  
  override fun delete(user: User?)
}
