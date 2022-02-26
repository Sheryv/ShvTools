package com.sheryv.tools.cloudservermanager.controller

import com.sheryv.tools.cloudservermanager.util.lg
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.SecureRandom
import kotlin.system.measureTimeMillis

@RestController
class RestTestController(private val encoder: PasswordEncoder) {
  
  @GetMapping("/test")
  fun test() {
    
    val pass = "pass"
    val rounds = 10
    time("built") {
      encoder.encode(pass)
    }
    
    time("default random") {
      val e = BCryptPasswordEncoder(rounds)
      e.encode(pass)
    }
    
    time("default random +1") {
      val e = BCryptPasswordEncoder(rounds + 1)
      e.encode(pass)
    }
    
    time("default random +2") {
      val e = BCryptPasswordEncoder(rounds + 2)
      e.encode(pass)
    }
    
    time("default random +6") {
      val e = BCryptPasswordEncoder(rounds + 6)
      e.encode(pass)
    }
    
    time("random: \"Windows-PRNG\"") {
      val e = BCryptPasswordEncoder(rounds, SecureRandom.getInstance("Windows-PRNG"))
      e.encode(pass)
    }
    
    time("random: \"SHA1PRNG\"") {
      val e = BCryptPasswordEncoder(rounds, SecureRandom.getInstance("SHA1PRNG"))
      e.encode(pass)
    }
    
    time("random: \"DRBG\"") {
      val e = BCryptPasswordEncoder(rounds, SecureRandom.getInstance("DRBG"))
      e.encode(pass)
    }
    
  }
  
  private fun time(name: String, block: () -> Unit) {
    val ms = measureTimeMillis {
      block()
    }
    lg().info("Duration: ${name.padEnd(25)} --> $ms ms")
  }
}
