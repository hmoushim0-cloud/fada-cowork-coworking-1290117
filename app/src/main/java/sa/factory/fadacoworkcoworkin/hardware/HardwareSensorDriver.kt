package sa.factory.fadacoworkcoworkin.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.fadacoworkcoworkin.engine.RfidTurnstileAccessProtocolEngine
import sa.factory.fadacoworkcoworkin.engine.SmartSpaceReservationEngine
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver(
    private val rfidEngine: RfidTurnstileAccessProtocolEngine = RfidTurnstileAccessProtocolEngine(),
    private val spaceEngine: SmartSpaceReservationEngine = SmartSpaceReservationEngine()
) {
    private val peripheral = "13.56 MHz RFID Turnstile Access + TOTP Pass"

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "ISO 14443-A NFC / MIFARE 13.56 MHz Reader Initializing..."))
        delay(250)

        // Initial Gate and TOTP Telemetry
        val initialTotp = spaceEngine.generateTotpPass("FADA-SECRET-KEY-99")
        val initialSpace = spaceEngine.evaluateSpaceTelemetry(activeHeadcount = 64, rawCo2 = 610, rawNoise = 48.2)

        emit(HardwareTelemetryFrame(
            peripheral,
            "ONLINE",
            "Turnstile #01 [OK] | TOTP Pass: $initialTotp | Occupancy: ${String.format("%.1f", initialSpace.occupancyRatePct)}% (${initialSpace.availableWorkstations} Desks Avail) | CO2: ${initialSpace.indoorCo2Ppm} ppm"
        ))

        var frameCounter = 200
        while (true) {
            delay(1500)
            frameCounter++

            val activeMembers = 60 + Random.nextInt(0, 15)
            val co2Reading = 580 + Random.nextInt(-30, 70)
            val noiseReading = 45.0 + Random.nextDouble(0.0, 6.0)

            val spaceTelem = spaceEngine.evaluateSpaceTelemetry(activeMembers, co2Reading, noiseReading)
            val rollingTotp = spaceEngine.generateTotpPass("FADA-SECRET-KEY-99")

            val livePayload = "Gate Flap: ARMED [OK] | Rolling TOTP: $rollingTotp (30s) | Desks: ${spaceTelem.availableWorkstations}/${SmartSpaceReservationEngine.TOTAL_WORKSTATIONS} | Ambient: ${String.format("%.1f", spaceTelem.ambientNoiseDba)} dBA | CO2: ${spaceTelem.indoorCo2Ppm} ppm"

            emit(HardwareTelemetryFrame(
                peripheralName = peripheral,
                connectionState = "ACTIVE_STREAMING",
                livePayload = livePayload
            ))
        }
    }
}
