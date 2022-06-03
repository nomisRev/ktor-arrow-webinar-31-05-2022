package io.github.nomisrev.service

import arrow.core.Either
import arrow.core.continuations.either
import io.github.nomisrev.DomainError
import io.github.nomisrev.persistence.UserPersistence
import io.github.nomisrev.validate

data class RegisterUser(val username: String, val email: String, val password: String)

object UserService {
  context(UserPersistence, JwtService)
  suspend fun register(input: RegisterUser): Either<DomainError, JwtToken> =
    either {
      val (username, email, password) = input.validate().bind()
      val userId = insert(username, email, password).bind()
      generateJwtToken(userId).bind()
    }
}
