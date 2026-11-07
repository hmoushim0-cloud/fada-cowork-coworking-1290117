package sa.factory.fadacoworkcoworkin.data.repository

import sa.factory.fadacoworkcoworkin.domain.repository.FadaCoworkRepository
import sa.factory.fadacoworkcoworkin.hardware.HardwareSensorDriver
import sa.factory.fadacoworkcoworkin.model.FadaCoworkItem
import sa.factory.fadacoworkcoworkin.model.FadaCoworkOrder
import sa.factory.fadacoworkcoworkin.model.FadaCoworkTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FadaCoworkRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : FadaCoworkRepository {

    private val _catalog = MutableStateFlow<List<FadaCoworkItem>>(listOf(
        FadaCoworkItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع 13.56 MHz RFID Turnstile Access + TOTP Pass", 180.0, "معتمد", "الخدمات الرئيسية"),
        FadaCoworkItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        FadaCoworkItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<FadaCoworkItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<FadaCoworkItem>>(emptyList())
    override val activeCart: StateFlow<List<FadaCoworkItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<FadaCoworkTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            FadaCoworkTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: FadaCoworkItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: FadaCoworkItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): FadaCoworkOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = FadaCoworkOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
