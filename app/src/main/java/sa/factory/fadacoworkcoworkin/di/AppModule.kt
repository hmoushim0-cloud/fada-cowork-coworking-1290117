package sa.factory.fadacoworkcoworkin.di

import sa.factory.fadacoworkcoworkin.data.repository.FadaCoworkRepositoryImpl
import sa.factory.fadacoworkcoworkin.domain.repository.FadaCoworkRepository
import sa.factory.fadacoworkcoworkin.domain.usecase.*
import sa.factory.fadacoworkcoworkin.hardware.HardwareSensorDriver
import sa.factory.fadacoworkcoworkin.ui.viewmodel.FadaCoworkViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: FadaCoworkRepository = FadaCoworkRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): FadaCoworkViewModel = FadaCoworkViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
