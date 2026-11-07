package sa.factory.fadacoworkcoworkin.data.remote

import sa.factory.fadacoworkcoworkin.model.FadaCoworkItem
import sa.factory.fadacoworkcoworkin.model.FadaCoworkOrder

interface FadaCoworkApiService {
    suspend fun fetchCatalog(): List<FadaCoworkItem>
    suspend fun submitOrder(order: FadaCoworkOrder): Boolean
}
