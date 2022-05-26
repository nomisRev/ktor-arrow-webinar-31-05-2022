package io.github.nomisrev

import io.github.nomisrev.env.Env
import io.github.nomisrev.env.hikari
import io.kotest.core.TestConfiguration

/**
 * A singleton `PostgreSQLContainer` Test Container. There is no need to `close` or `stop` the
 * test-container since the lifecycle is controlled by TC Ryuk container.
 *
 * ```kotlin
 * class TestClass : StringSpec({
 *   val postgres = PostgreSQLContainer.create()
 *   ...
 * })
 * ```
 *
 * // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/
 */
class PostgreSQLContainer private constructor() :
  org.testcontainers.containers.PostgreSQLContainer<PostgreSQLContainer>("postgres:14.1-alpine") {

  fun dataSource(): Env.DataSource =
    Env.DataSource(jdbcUrl, username, password, driverClassName)

  companion object {
    fun create(): PostgreSQLContainer = instance

    fun dataSource(): Env.DataSource = instance.dataSource()

    private val instance by lazy { PostgreSQLContainer().also { it.start() } }
  }
}
