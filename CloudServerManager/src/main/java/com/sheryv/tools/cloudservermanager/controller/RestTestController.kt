package com.sheryv.tools.cloudservermanager.controller

import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.PathType
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.DefaultQueryHandler
import com.querydsl.jpa.impl.JPAQueryFactory
import com.querydsl.jpa.sql.JPASQLQuery
import com.querydsl.sql.Configuration
import com.querydsl.sql.PostgreSQLTemplates
import com.querydsl.sql.SQLQueryFactory
import com.sheryv.shvtools.cloudservermanager.AuthoritiesTable
import com.sheryv.shvtools.cloudservermanager.UsersAuthoritiesTable
import com.sheryv.shvtools.cloudservermanager.UsersTable
import com.sheryv.tools.cloudservermanager.entities.QAuthority
import com.sheryv.tools.cloudservermanager.entities.QUser
import com.sheryv.tools.cloudservermanager.entities.UserEntity
import com.sheryv.tools.cloudservermanager.repository.UserRepository
import com.sheryv.tools.cloudservermanager.util.lg
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.SecureRandom
import javax.persistence.EntityManager
import javax.persistence.Tuple
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

@RestController
class RestTestController(
  private val encoder: PasswordEncoder,
  private val enityManager: EntityManager,
  private val userRepository: UserRepository,
  private val data: DataSource
) {
  
  @GetMapping("/test")
  fun test() {
    PostgreSQLTemplates.builder()
    val j = JPAQueryFactory(enityManager)
    
    val s = SQLQueryFactory(Configuration(PostgreSQLTemplates.builder().build()), data)
    
    val u = QUser.user
    val a = QAuthority("a")
    val wh = u.login.eq("admin")
    val fetch = j.select(u.id, u.login, u.password).from(u).join(u.roles).where(wh).fetch()
    val list = enityManager.createQuery("select u.id as id, u.login as login, u.password as pass from UserEntity u", Tuple::class.java).resultList
    val t = UsersTable("user")
    val tua = UsersAuthoritiesTable.usersAuthorities
    val ta = AuthoritiesTable.authorities
    lg().info("second")
    val sec = JPASQLQuery<UserEntity>(enityManager, Configuration(PostgreSQLTemplates.builder().build()), DefaultQueryHandler.DEFAULT)
//      .select(u.id, u.login, u.isEnabled, a.code)
      .select(t)
      .from(t)
      .join(tua).on(t.id.eq(tua.userId))
      .join(ta).on(ta.id.eq(tua.authorityId))
      .where(t.username.eq("admin")).fetch()
    lg().info("third")
//    val res = j.selectFrom(u).join(u.roles).fetchJoin().where(wh).fetchResults()
    lg().info("ff")
    val sql = s.select(*t.all()).from(t).where(t.username.eq("admin"))
    lg().info(sql.sql.sql)
    val first = sql.fetchOne()
  
    val path = PathBuilder(UserEntity::class.java, "users")
    val fetch1 =
//      s.select(path.get("username"), path.get("password"), path.get("id"))
      s.select(path.get("username"), path.get("password"))
        .from(path)
        .where(path.get("username").eq("admin"))
        .fetch()
    
  
    val pass = "pass"
    val rounds = 10
    time("built") {
      encoder.encode(pass)
    }
    
    time("default random") {
      val e = BCryptPasswordEncoder(rounds)
      e.encode(pass)
    }

//    time("default random +1") {
//      val e = BCryptPasswordEncoder(rounds + 1)
//      e.encode(pass)
//    }
//
//    time("default random +2") {
//      val e = BCryptPasswordEncoder(rounds + 2)
//      e.encode(pass)
//    }

//    time("default random +6") {
//      val e = BCryptPasswordEncoder(rounds + 6)
//      e.encode(pass)
//    }
//
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
