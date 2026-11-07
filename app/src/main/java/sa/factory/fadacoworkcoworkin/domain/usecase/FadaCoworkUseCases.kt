package sa.factory.fadacoworkcoworkin.domain.usecase

import sa.factory.fadacoworkcoworkin.domain.repository.FadaCoworkRepository
import sa.factory.fadacoworkcoworkin.model.FadaCoworkItem
import sa.factory.fadacoworkcoworkin.model.FadaCoworkOrder
import sa.factory.fadacoworkcoworkin.model.FadaCoworkTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: FadaCoworkRepository) {
    operator fun invoke(): Flow<List<FadaCoworkItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: FadaCoworkRepository) {
    val cart = repository.activeCart
    fun add(item: FadaCoworkItem) = repository.addToCart(item)
    fun remove(item: FadaCoworkItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: FadaCoworkRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): FadaCoworkOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: FadaCoworkRepository) {
    operator fun invoke(): Flow<FadaCoworkTelemetry> = repository.telemetryFlow
}
