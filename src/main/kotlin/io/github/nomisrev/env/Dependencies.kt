package io.github.nomisrev.env

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.resource
import io.github.nomisrev.persistence.UserPersistence
import io.github.nomisrev.persistence.userPersistence
import io.github.nomisrev.service.JwtService

class Dependencies(val userPersistence: UserPersistence, val jwtService: JwtService)

fun dependencies(env: Env): Resource<Dependencies> = resource {
  val hikari = hikari(env.dataSource).bind()
  val sqlDelight = sqlDelight(hikari).bind()
  Dependencies(
    userPersistence(sqlDelight.usersQueries),
    JwtService(env.auth)
  )
}
