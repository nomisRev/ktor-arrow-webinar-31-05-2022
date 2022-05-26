package io.github.nomisrev.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import io.github.nomisrev.IncorrectInput
import io.github.nomisrev.InvalidPassword
import io.github.nomisrev.InvalidUsername
import io.github.nomisrev.InvalidEmail
import io.github.nomisrev.UserError
import io.github.nomisrev.JwtGeneration
import io.github.nomisrev.persistence.UserId
import io.github.nomisrev.persistence.UserPersistence
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FreeSpec

class UserServiceUnitSpec : FreeSpec({

  val validUsername = "username"
  val validEmail = "valid@domain.com"
  val validPw = "123456789"

  fun userService(
    userPersistence: UserPersistence = stubUserPersistence(),
    jwtService: JwtService = JwtService { TODO() }
  ) = io.github.nomisrev.service.userService(userPersistence, jwtService)

  "register" - {
    "username cannot be empty" {
      val res = userService().register(RegisterUser("", validEmail, validPw))
      val errors = nonEmptyListOf("Cannot be blank", "is too short (minimum is 1 characters)")
      val expected = IncorrectInput(InvalidUsername(errors))
      res shouldBeLeft expected
    }

    "username longer than 25 chars" {
      val name = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      val res = userService().register(RegisterUser(name, validEmail, validPw))
      val errors = nonEmptyListOf("is too long (maximum is 25 characters)")
      val expected = IncorrectInput(InvalidUsername(errors))
      res shouldBeLeft expected
    }

    "email cannot be empty" {
      val res = userService().register(RegisterUser(validUsername, "", validPw))
      val errors = nonEmptyListOf("Cannot be blank", "'' is invalid email")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "email too long" {
      val email = "${(0..340).joinToString("") { "A" }}@domain.com"
      val res = userService().register(RegisterUser(validUsername, email, validPw))
      val errors = nonEmptyListOf("is too long (maximum is 350 characters)")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "email is not valid" {
      val email = "AAAA"
      val res = userService().register(RegisterUser(validUsername, email, validPw))
      val errors = nonEmptyListOf("'$email' is invalid email")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "password cannot be empty" {
      val res = userService().register(RegisterUser(validUsername, validEmail, ""))
      val errors = nonEmptyListOf("Cannot be blank", "is too short (minimum is 8 characters)")
      val expected = IncorrectInput(InvalidPassword(errors))
      res shouldBeLeft expected
    }

    "password can be max 100" {
      val password = (0..100).joinToString("") { "A" }
      val res = userService().register(RegisterUser(validUsername, validEmail, password))
      val errors = nonEmptyListOf("is too long (maximum is 100 characters)")
      val expected = IncorrectInput(InvalidPassword(errors))
      res shouldBeLeft expected
    }

    "All valid returns a token" {
      val token = JwtToken("value")
      userService(stubUserPersistence { _, _, _ -> UserId(1).right() }) { token.right() }
        .register(RegisterUser(validUsername, validEmail, validPw))
        .shouldBeRight(token)
    }

    "JwtGeneration fails" {
      val error = JwtGeneration("Failed")
      userService(stubUserPersistence { _, _, _ -> UserId(1).right() }) { error.left() }
        .register(RegisterUser(validUsername, validEmail, validPw))
        .shouldBeLeft(error)
    }
  }
})

fun stubUserPersistence(
  insert: suspend (String, String, String) -> Either<UserError, UserId> = { _, _, _ -> TODO() }
) = object : UserPersistence {
  override suspend fun insert(username: String, email: String, password: String): Either<UserError, UserId> =
    insert(username, email, password)
}
