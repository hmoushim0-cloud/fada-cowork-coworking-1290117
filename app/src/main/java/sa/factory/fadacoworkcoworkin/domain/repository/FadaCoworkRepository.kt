package sa.factory.fadacoworkcoworkin.domain.repository

import sa.factory.fadacoworkcoworkin.model.FadaCoworkItem
import sa.factory.fadacoworkcoworkin.model.FadaCoworkOrder
import sa.factory.fadacoworkcoworkin.model.FadaCoworkTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FadaCoworkRepository {
    val catalogFlow: Flow<List<FadaCoworkItem>>
    val activeCart: StateFlow<List<FadaCoworkItem>>
    val telemetryFlow: Flow<FadaCoworkTelemetry>
    
    fun addToCart(item: FadaCoworkItem)
    fun removeFromCart(item: FadaCoworkItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): FadaCoworkOrder
    suspend fun refreshCatalog()
}
