package com.sheryv.tools.cloudservermanager.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.sheryv.tools.cloudservermanager.model.Authorities
import com.sheryv.tools.cloudservermanager.service.UserLoginService
import com.sheryv.tools.cloudservermanager.util.lg
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.util.MimeTypeUtils
import java.security.SecureRandom
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
open class SecSecurityConfig(
  private val loginService: UserLoginService
) : WebSecurityConfigurerAdapter() {
//  private val publicUrls = OrRequestMatcher(
//      AntPathRequestMatcher("/"),
//      AntPathRequestMatcher("/public/**"),
//      AntPathRequestMatcher("/app/**"),
//      AntPathRequestMatcher("/app")
////            AntPathRequestMatcher("/assets/**")
//  )

//  @Bean
//  @Throws(Exception::class)
//  override fun authenticationManagerBean(): AuthenticationManager {
//    return super.authenticationManagerBean();
//  }

//    override fun configure(web: WebSecurity) {
//        web.ignoring().requestMatchers(publicUrls)
//    }

//    @Bean
//    fun authenticationEntryPoint(): AuthenticationEntryPoint {
//        return AuthenticationEntryPoint { _, response, _ ->
//            println("\n" +
//                    "\nauthenticationEntryPoint\n\n")
////                    response.status = 403
//        }
//    }
  
  override fun userDetailsService(): UserDetailsService {
    return loginService
  }


  override fun configure(auth: AuthenticationManagerBuilder) {
    auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder())
  }


//  override fun configure(auth: AuthenticationManagerBuilder) {
//    auth.inMemoryAuthentication()
//      .withUser("user1").password(passwordEncoder().encode("pass")).roles(*Authorities.userRoles())
//      .and()
//      .withUser("user2").password(passwordEncoder().encode("pass")).roles(*Authorities.moderatorRoles())
//      .and()
//      .withUser("admin").password(passwordEncoder().encode("pass")).roles(*Authorities.adminRoles())
//  }
//
  
  @Throws(Exception::class)
  override fun configure(http: HttpSecurity) {
//    val protectedUrls = NegatedRequestMatcher(publicUrls)
    http
      .csrf().disable()
      .authorizeRequests()
      .antMatchers("/admin/**").hasRole(Authorities.ADMIN_EDIT.code)
      .antMatchers("/", "/test").permitAll()
      .anyRequest().authenticated()
      .and()
      .formLogin()
      .loginPage("/login")
      .defaultSuccessUrl("/panel")
      .permitAll()
      .and()
      .logout()
      .logoutUrl("/logout")
      .deleteCookies("JSESSIONID")
      .and()
      .httpBasic().disable()
//      .exceptionHandling()
//                .accessDeniedHandler { _, response, _ ->
//                    println("accessDeniedHandler")
//                    response.status = 403
//                }
//                .authenticationEntryPoint (RestAuthenticationEntryPoint())
  }

//
//  fun tokenAuthenticationFilter(): TokenAuthenticationFilter {
//    val protectedUrls = NegatedRequestMatcher(publicUrls)
//    val filter = TokenAuthenticationFilter(protectedUrls)
//    filter.setAuthenticationManager(authenticationManager())
//    filter.setAuthenticationSuccessHandler(successHandler())
//    return filter
//  }
//
//  @Bean
//  fun successHandler(): SimpleUrlAuthenticationSuccessHandler {
//    val successHandler = SimpleUrlAuthenticationSuccessHandler()
//    successHandler.setRedirectStrategy(NoRedirectStrategy())
//    return successHandler
//  }
//
  /**
   * Disable Spring boot automatic filter registration.
   */
//    @Bean
//    fun disableAutoRegistration(filter: TokenAuthenticationFilter): FilterRegistrationBean<*> {
//        val registration = FilterRegistrationBean(filter)
//        registration.isEnabled = false
//        return registration
//    }
  
  @Bean
  open fun passwordEncoder(): PasswordEncoder {
    
    lg().info("Before")
    val s = SecureRandom.getInstance("Windows-PRNG")
    val byteArray = ByteArray(10000)
    s.nextBytes(byteArray)
    lg().info("After 1 " + byteArray[0])
    
    val s2 = SecureRandom()
    s2.nextBytes(byteArray)
    lg().info("After 2 " + byteArray[0])
    
    return BCryptPasswordEncoder(4, s)
  }
  
}

internal class NoRedirectStrategy : RedirectStrategy {
  override fun sendRedirect(request: HttpServletRequest?, response: HttpServletResponse?, url: String?) {
  }
}


class AuthFilter : UsernamePasswordAuthenticationFilter() {
  override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
    return super.attemptAuthentication(request, response)
  }
}

class TokenAuthenticationFilter(requiresAuth: RequestMatcher) : AbstractAuthenticationProcessingFilter(requiresAuth) {
  
  init {
    setAuthenticationFailureHandler { request, response, exception ->
      
      when (exception.javaClass) {
        CredentialsExpiredException::class.java -> {
          val mapper = ObjectMapper()
          response?.let {
            it.reset()
            val s = mapper.writeValueAsString("Session expired")
            it.writer.print(s)
            it.status = HttpServletResponse.SC_UNAUTHORIZED
          }
        }
        else -> {
          val mapper = ObjectMapper()
          response?.let {
            it.reset()
            val s = mapper.writeValueAsString("" + exception.message)
            it.writer.print(s)
            it.status = HttpServletResponse.SC_UNAUTHORIZED
          }
        }
      }
      response.contentType = MimeTypeUtils.APPLICATION_JSON_VALUE
      
      
    }
  }
  
  override fun attemptAuthentication(
    request: HttpServletRequest,
    response: HttpServletResponse
  ): Authentication? {
    if (SecurityContextHolder.getContext().authentication != null) {
      return SecurityContextHolder.getContext().authentication
    }
    if (request.userPrincipal != null) {
      return null
//            return SecurityContextHolder.getContext().getAuthentication()
    }
    val key: String
    val authorities: Set<SimpleGrantedAuthority>
//    if (!request.getParameter("api_key").isNullOrBlank() || !request.getHeader("X-API-Key").isNullOrBlank()) {
//      key = request.getParameter("api_key") ?: request.getHeader("X-API-Key")
//      authorities = setOf(Authority.TYPE_API)
//    } else {
//      val param: String = (request.getHeader(HttpHeaders.AUTHORIZATION)?.let { DataUtils.getTokenFromHeader(it) }
//          ?: request.getParameter("token"))
//          ?: throw BadCredentialsException("Missing Authentication Token")
//      key = param.trim()
//      authorities = setOf(Authority.TYPE_TOKEN)
//    }
//
//    val auth = UsernamePasswordAuthenticationToken("token:" + key.take(5), key, authorities)
//    return authenticationManager.authenticate(auth)
    return null
  }
  
  override fun successfulAuthentication(
    request: HttpServletRequest,
    response: HttpServletResponse,
    chain: FilterChain,
    authResult: Authentication
  ) {
    super.successfulAuthentication(request, response, chain, authResult)
    chain.doFilter(request, response)
  }
  
}
