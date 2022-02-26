package com.sheryv.tools.cloudservermanager.service

import com.sheryv.tools.cloudservermanager.entities.Authority
import com.sheryv.tools.cloudservermanager.entities.User
import com.sheryv.tools.cloudservermanager.model.Authorities
import com.sheryv.tools.cloudservermanager.repository.AuthorityRepository
import com.sheryv.tools.cloudservermanager.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.crypto.password.PasswordEncoder
import javax.transaction.Transactional

@Configuration
open class SetupDataLoader(
  private val authorityRepository: AuthorityRepository,
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder
) : ApplicationListener<ContextRefreshedEvent?> {
  private var alreadySetup = false
  
  // API
  @Transactional
  override fun onApplicationEvent(event: ContextRefreshedEvent?) {
    if (alreadySetup) {
      return
    }
    var adminRole: Authority? = null
    var roleUser: Authority? = null
    Authorities.values().forEach {
      val a = createAuthorityIfNotFound(it)
      if (it == Authorities.ADMIN_EDIT)
        adminRole = a
      if (it == Authorities.USER_EDIT)
        roleUser = a
    }
    createUserIfNotFound("admin@test.com", "admin", "AdminTest", "Test", "pass", listOf(adminRole!!))
    createUserIfNotFound("user@test.com", "userr", "UserTest", "UTest", "pass", listOf(roleUser!!))
    alreadySetup = true
  }
  
  @Transactional
  open fun createAuthorityIfNotFound(name: Authorities): Authority {
    var role: Authority? = authorityRepository.findByCode(name.code)
    return if (role == null) {
      role = Authority(code = name.code)
      role = authorityRepository.save(role)
      role
    } else
      role
  }
  
  @Transactional
  open fun createUserIfNotFound(
    email: String?,
    username: String?,
    firstName: String?,
    lastName: String?,
    password: String?,
    roles: Collection<Authority>
  ): User {
    var user: User? = userRepository.findByUsername(username)
    if (user == null) {
      user = User()
      user.username = username
      user.password = passwordEncoder.encode(password)
      user.roles = roles
      
      user = userRepository.save(user)
    }
    return user!!
  }
  
}
