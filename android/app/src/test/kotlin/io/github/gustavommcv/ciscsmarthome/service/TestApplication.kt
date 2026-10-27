package io.github.gustavommcv.ciscsmarthome.service

import android.app.Application
import io.github.gustavommcv.ciscsmarthome.AppDependencies
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository

/** The application of the service tests: what the service asks of it is a fake repository the test moves by hand. */
class TestApplication : Application(), AppDependencies {
  val repository = FakeHomeRepository()

  override val homeRepository: HomeRepository get() = repository

  override fun startMonitoring() = Unit
}
