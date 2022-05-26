package io.github.nomisrev.service

import arrow.core.Either
import arrow.core.continuations.EffectScope
import io.github.nefilim.kjwt.JWSAlgorithm
import io.github.nefilim.kjwt.JWT
import io.github.nefilim.kjwt.KJWTSignError
import io.github.nefilim.kjwt.SignedJWT
import io.github.nefilim.kjwt.sign
import io.github.nomisrev.JwtGeneration
import io.github.nomisrev.env.Env
import io.github.nomisrev.persistence.UserId
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.toJavaDuration

@JvmInline
value class JwtToken(val value: String)

interface JwtService {
  /** Generate a new JWT token for userId and password. Doesn't invalidate old password */
  context(EffectScope<JwtGeneration>) // TODO report bug fun interface + context
  suspend fun generateJwtToken(userId: UserId): JwtToken
}

@Suppress("FUNCTION_NAME")
fun JwtService(auth: Env.Auth): JwtService = object : JwtService {
  context(EffectScope<JwtGeneration>)
  override suspend fun generateJwtToken(userId: UserId): JwtToken =
    JWT.hs512 {
    val now = LocalDateTime.now(ZoneId.of("UTC"))
    issuedAt(now)
    expiresAt(now + auth.duration.toJavaDuration())
    issuer(auth.issuer)
    claim("id", userId.serial)
  }
    .sign(auth.secret)
    .bind()
    .let { JwtToken(it.rendered) }
}

context(EffectScope<JwtGeneration>)
private suspend fun <A : JWSAlgorithm> Either<KJWTSignError, SignedJWT<A>>.bind(): SignedJWT<A> =
  mapLeft { jwtError ->
    when (jwtError) {
      KJWTSignError.InvalidKey -> JwtGeneration("JWT singing error: invalid Secret Key.")
      KJWTSignError.InvalidJWTData ->
        JwtGeneration("JWT singing error: Generated with incorrect JWT data")
      is KJWTSignError.SigningError -> JwtGeneration("JWT singing error: ${jwtError.cause}")
    }
  }.bind()
