package com.sheryv.tools.cloudservermanager.service

import com.sheryv.tools.cloudservermanager.entities.User
import com.sheryv.tools.cloudservermanager.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class UserLoginService(
  private val userRepository: UserRepository
) : UserDetailsService {
  
  @Transactional
  override fun loadUserByUsername(username: String): UserDetails {
    return try {
      val user: User = userRepository.findByUsername(username) ?: throw UsernameNotFoundException("No user found with username: $username")
      
      org.springframework.security.core.userdetails.User(
        user.username,
        user.password, user.isEnabled, true,
        true, true, user.roles.map { it.toAuthority() }
      )
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }
}
