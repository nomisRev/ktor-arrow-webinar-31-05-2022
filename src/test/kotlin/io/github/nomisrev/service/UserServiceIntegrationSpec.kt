package io.github.nomisrev.service

import arrow.core.Either
import arrow.core.nonEmptyListOf
import io.github.nomisrev.with
import io.github.nomisrev.DomainError
import io.github.nomisrev.IncorrectInput
import io.github.nomisrev.InvalidEmail
import io.github.nomisrev.InvalidPassword
import io.github.nomisrev.InvalidUsername
import io.github.nomisrev.PostgreSQLContainer
import io.github.nomisrev.UsernameAlreadyExists
import io.github.nomisrev.env.Env
import io.github.nomisrev.env.dependencies
import io.github.nomisrev.env.hikari
import io.github.nomisrev.query
import io.github.nomisrev.resource
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FreeSpec

class UserServiceIntegrationSpec : FreeSpec({
  val env = Env().copy(dataSource = PostgreSQLContainer.dataSource())
  val dataSource by resource(hikari(env.dataSource))
  val dependencies by resource(dependencies(env))

  val validUsername = "username"
  val validEmail = "valid@domain.com"
  val validPw = "123456789"

  afterTest { dataSource.query("TRUNCATE users") }

  suspend fun register(input: RegisterUser): Either<DomainError, JwtToken> =
    with(dependencies.userPersistence, dependencies.jwtService) {
      UserService.register(input)
    }

  "register" - {
    "username cannot be empty" {
      val res = register(RegisterUser("", validEmail, validPw))
      val errors = nonEmptyListOf("Cannot be blank", "is too short (minimum is 1 characters)")
      val expected = IncorrectInput(InvalidUsername(errors))
      res shouldBeLeft expected
    }

    "username longer than 25 chars" {
      val name = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      val res = register(RegisterUser(name, validEmail, validPw))
      val errors = nonEmptyListOf("is too long (maximum is 25 characters)")
      val expected = IncorrectInput(InvalidUsername(errors))
      res shouldBeLeft expected
    }

    "email cannot be empty" {
      val res = register(RegisterUser(validUsername, "", validPw))
      val errors = nonEmptyListOf("Cannot be blank", "'' is invalid email")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "email too long" {
      val email = "${(0..340).joinToString("") { "A" }}@domain.com"
      val res = register(RegisterUser(validUsername, email, validPw))
      val errors = nonEmptyListOf("is too long (maximum is 350 characters)")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "email is not valid" {
      val email = "AAAA"
      val res = register(RegisterUser(validUsername, email, validPw))
      val errors = nonEmptyListOf("'$email' is invalid email")
      val expected = IncorrectInput(InvalidEmail(errors))
      res shouldBeLeft expected
    }

    "password cannot be empty" {
      val res = register(RegisterUser(validUsername, validEmail, ""))
      val errors = nonEmptyListOf("Cannot be blank", "is too short (minimum is 8 characters)")
      val expected = IncorrectInput(InvalidPassword(errors))
      res shouldBeLeft expected
    }

    "password can be max 100" {
      val password = (0..100).joinToString("") { "A" }
      val res = register(RegisterUser(validUsername, validEmail, password))
      val errors = nonEmptyListOf("is too long (maximum is 100 characters)")
      val expected = IncorrectInput(InvalidPassword(errors))
      res shouldBeLeft expected
    }

    "All valid returns a token" {
      register(RegisterUser(validUsername, validEmail, validPw)).shouldBeRight()
    }

    "Register twice results in" {
      register(RegisterUser(validUsername, validEmail, validPw)).shouldBeRight()
      val res = register(RegisterUser(validUsername, validEmail, validPw))
      res shouldBeLeft UsernameAlreadyExists(validUsername)
    }
  }
})
