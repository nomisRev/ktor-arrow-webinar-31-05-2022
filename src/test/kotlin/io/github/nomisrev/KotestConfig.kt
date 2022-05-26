package io.github.nomisrev

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.test.TestCaseOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object KotestConfig : AbstractProjectConfig() {
  override val timeout: Duration = 5.seconds
  override val globalAssertSoftly: Boolean = true
  override val testCaseOrder: TestCaseOrder = TestCaseOrder.Random
  override val coroutineDebugProbes: Boolean = true
}
