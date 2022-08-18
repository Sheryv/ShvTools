package com.sheryv.tools.cloudservermanager.repository

import com.sheryv.tools.cloudservermanager.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
  @Query("select u from UserEntity u left join fetch u.roles where u.login = ?1")
  fun findByUsername(username: String?): UserEntity?
  
  override fun delete(user: UserEntity)
}
