package io.github.nomisrev.service

import arrow.core.Either
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import arrow.core.continuations.either
import arrow.core.nonEmptyListOf
import io.github.nomisrev.DomainError
import io.github.nomisrev.DomainErrors
import io.github.nomisrev.IncorrectInput
import io.github.nomisrev.InvalidPassword
import io.github.nomisrev.InvalidUsername
import io.github.nomisrev.InvalidEmail
import io.github.nomisrev.UserError
import io.github.nomisrev.JwtGeneration
import io.github.nomisrev.TypePlacedHolder
import io.github.nomisrev.UserErrors
import io.github.nomisrev.persistence.UserId
import io.github.nomisrev.persistence.UserPersistence
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FreeSpec

class UserServiceUnitSpec : FreeSpec({

  val validUsername = "username"
  val validEmail = "valid@domain.com"
  val validPw = "123456789"

  "register" - {
    "username cannot be empty" {
      val res = userCtx { UserService.register(RegisterUser("", validEmail, validPw)) }
      val errors = nonEmptyListOf("Cannot be blank", "is too short (minimum is 1 characters)")
      val expected = IncorrectInput(InvalidUsername(errors))
      res shouldBeLeft expected
    }

    "username longer than 25 chars" {
      val name = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      val res = userCtx { UserService.register(RegisterUser(name, validEmail, validPw)) }
      val errors = nonEmptyListOf("is too long (maximum is 25 characters)")
      val expected = IncorrectInput(InvalidUsername(errors))
      res shouldBeLeft expected
    }

    "email cannot be empty" {
      val res = userCtx { UserService.register(RegisterUser(validUsername, "", validPw)) }
      val errors = nonEmptyListOf("Cannot be blank", "'' is invalid email")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "email too long" {
      val email = "${(0..340).joinToString("") { "A" }}@domain.com"
      val res = userCtx { UserService.register(RegisterUser(validUsername, email, validPw)) }
      val errors = nonEmptyListOf("is too long (maximum is 350 characters)")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "email is not valid" {
      val email = "AAAA"
      val res = userCtx { UserService.register(RegisterUser(validUsername, email, validPw)) }
      val errors = nonEmptyListOf("'$email' is invalid email")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "password cannot be empty" {
      val res = userCtx { UserService.register(RegisterUser(validUsername, validEmail, "")) }
      val errors = nonEmptyListOf("Cannot be blank", "is too short (minimum is 8 characters)")
      val expected = IncorrectInput(InvalidPassword(errors))
      res shouldBeLeft expected
    }

    "password can be max 100" {
      val password = (0..100).joinToString("") { "A" }
      val res = userCtx { UserService.register(RegisterUser(validUsername, validEmail, password)) }
      val errors = nonEmptyListOf("is too long (maximum is 100 characters)")
      val expected = IncorrectInput(InvalidPassword(errors))
      res shouldBeLeft expected
    }

    "All valid returns a token" {
      val token = JwtToken("value")
      userCtx(stubUserPersistence { _, _, _ -> UserId(1) }, stubJwtService { token }) {
        UserService.register(RegisterUser(validUsername, validEmail, validPw))
      }.shouldBeRight(token)
    }

    "JwtGeneration fails" {
      val error = JwtGeneration("Failed")
      userCtx(stubUserPersistence { _, _, _ -> UserId(1) }, stubJwtService { shift(error) }) {
        UserService.register(RegisterUser(validUsername, validEmail, validPw))
      }.shouldBeLeft(error)
    }
  }
})

private fun stubUserPersistence(
  insertUser: suspend context(UserErrors) (String, String, String) -> UserId = { _, _, _ -> TODO() }
) = object : UserPersistence {
  context(UserErrors)
  override suspend fun insert(username: String, email: String, password: String): UserId =
    // TODO bug report, we should be able to call insert here without additional wrapping
    effect<UserError, UserId> {
      insertUser(this, username, email, password)
    }.bind()
}

private fun stubJwtService(
  generateToken: suspend context(EffectScope<JwtGeneration>) (UserId) -> JwtToken = { TODO() }
): JwtService = object: JwtService {
  context(EffectScope<JwtGeneration>)
  override suspend fun generateJwtToken(userId: UserId): JwtToken =
    effect<JwtGeneration, JwtToken> {
      generateToken(this, userId)
    }.bind()
}

private suspend fun <A> userCtx(
  userPersistence: UserPersistence = stubUserPersistence(),
  jwtService: JwtService = stubJwtService(),
  block: suspend context(UserPersistence, JwtService, DomainErrors) (TypePlacedHolder<DomainErrors>) -> A
): Either<DomainError, A> = either {
    block(userPersistence, jwtService, this, TypePlacedHolder)
}
