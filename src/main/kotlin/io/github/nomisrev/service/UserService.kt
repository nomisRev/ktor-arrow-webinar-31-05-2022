package io.github.nomisrev.service

import io.github.nomisrev.DomainErrors
import io.github.nomisrev.persistence.UserPersistence
import io.github.nomisrev.validate

data class RegisterUser(val username: String, val email: String, val password: String)

object UserService {
  context(UserPersistence, JwtService, DomainErrors)
  suspend fun register(input: RegisterUser): JwtToken {
    val (username, email, password) = input.validate().bind()
    val userId = insert(username, email, password)
    return generateJwtToken(userId)
  }
}
