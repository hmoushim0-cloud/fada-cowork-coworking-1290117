package sa.factory.fadacoworkcoworkin

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.fadacoworkcoworkin.data.repository.FadaCoworkRepositoryImpl
import sa.factory.fadacoworkcoworkin.engine.RfidTurnstileAccessProtocolEngine
import sa.factory.fadacoworkcoworkin.engine.SmartSpaceReservationEngine
import sa.factory.fadacoworkcoworkin.hardware.HardwareSensorDriver
import sa.factory.fadacoworkcoworkin.model.FadaCoworkItem

class FadaCoworkRegressionTest {

    private val rfidEngine = RfidTurnstileAccessProtocolEngine()
    private val spaceEngine = SmartSpaceReservationEngine()

    @Test
    fun test01_cardUidChecksumValidation() {
        // 4-byte UID: 0x04, 0xA2, 0x5B, 0x1F. BCC = 0x04 ^ 0xA2 ^ 0x5B ^ 0x1F = 0xE2
        val validUid = byteArrayOf(0x04, 0xA2.toByte(), 0x5B, 0x1F, 0xE2.toByte())
        assertTrue(rfidEngine.validateCardUid(validUid))

        // Corrupted BCC
        val corruptedUid = byteArrayOf(0x04, 0xA2.toByte(), 0x5B, 0x1F, 0x00)
        assertFalse(rfidEngine.validateCardUid(corruptedUid))
    }

    @Test
    fun test02_apduCommandSerialization() {
        val apdu = rfidEngine.buildApduCommand(
            cla = 0x00,
            ins = RfidTurnstileAccessProtocolEngine.INS_SELECT,
            p1 = 0x04,
            p2 = 0x00,
            data = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x03, 0x96.toByte())
        )

        assertEquals(10, apdu.size)
        assertEquals(0x00, apdu[0].toInt())
        assertEquals(RfidTurnstileAccessProtocolEngine.INS_SELECT, apdu[1].toInt() and 0xFF)
        assertEquals(0x04, apdu[2].toInt())
        assertEquals(0x00, apdu[3].toInt())
        assertEquals(5, apdu[4].toInt()) // Lc
    }

    @Test
    fun test03_turnstileAccessGrantedForHotDesk() {
        rfidEngine.resetAntiPassbackLedger()
        val result = rfidEngine.evaluateTurnstileTap(
            cardUidHex = "04A25B1FE2",
            memberId = "MEM-8821",
            tier = RfidTurnstileAccessProtocolEngine.AccessTier.HOT_DESK,
            direction = RfidTurnstileAccessProtocolEngine.GateDirection.ENTRY,
            gateId = "TURNSTILE-GATE-01"
        )

        assertTrue(result is RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessGranted)
        val granted = result as RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessGranted
        assertEquals("MEM-8821", granted.memberId)
        assertEquals(RfidTurnstileAccessProtocolEngine.GateDirection.ENTRY, granted.direction)
        assertEquals("TURNSTILE-GATE-01", granted.gateId)
    }

    @Test
    fun test04_antiPassbackEnforcementBlocksDoubleEntry() {
        rfidEngine.resetAntiPassbackLedger()
        
        // First entry: Granted
        val firstResult = rfidEngine.evaluateTurnstileTap(
            cardUidHex = "04A25B1FE2",
            memberId = "MEM-8821",
            tier = RfidTurnstileAccessProtocolEngine.AccessTier.DEDICATED_DESK,
            direction = RfidTurnstileAccessProtocolEngine.GateDirection.ENTRY
        )
        assertTrue(firstResult is RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessGranted)

        // Immediate second entry without an exit: Blocked by APB rule
        val secondResult = rfidEngine.evaluateTurnstileTap(
            cardUidHex = "04A25B1FE2",
            memberId = "MEM-8821",
            tier = RfidTurnstileAccessProtocolEngine.AccessTier.DEDICATED_DESK,
            direction = RfidTurnstileAccessProtocolEngine.GateDirection.ENTRY
        )
        assertTrue(secondResult is RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessDenied)
        val denied = secondResult as RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessDenied
        assertTrue(denied.isAntiPassbackViolation)
    }

    @Test
    fun test05_guestTurnstileEntryBlocked() {
        rfidEngine.resetAntiPassbackLedger()
        val result = rfidEngine.evaluateTurnstileTap(
            cardUidHex = "0411223344",
            memberId = "GUEST-101",
            tier = RfidTurnstileAccessProtocolEngine.AccessTier.GUEST,
            direction = RfidTurnstileAccessProtocolEngine.GateDirection.ENTRY
        )

        assertTrue(result is RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessDenied)
        val denied = result as RfidTurnstileAccessProtocolEngine.TurnstileAccessResult.AccessDenied
        assertFalse(denied.isAntiPassbackViolation)
        assertTrue(denied.reason.contains("Guest Visitor"))
    }

    @Test
    fun test06_totpRollingPassGeneration() {
        val secret = "TEST-FADA-SECRET-KEY"
        val fixedTimestamp = 1700000000000L // deterministic window

        val pass1 = spaceEngine.generateTotpPass(secret, fixedTimestamp)
        val pass2 = spaceEngine.generateTotpPass(secret, fixedTimestamp + 10_000L) // same 30s window
        val pass3 = spaceEngine.generateTotpPass(secret, fixedTimestamp + 35_000L) // next 30s window

        assertEquals(6, pass1.length)
        assertTrue(pass1.all { it.isDigit() })
        assertEquals(pass1, pass2) // within same time window
        assertNotEquals(pass1, pass3) // advances after 30s
    }

    @Test
    fun test07_totpPassDriftVerification() {
        val secret = "TEST-FADA-SECRET-KEY"
        val currentTime = 1700000000000L
        val currentPass = spaceEngine.generateTotpPass(secret, currentTime)

        // Valid on current timestamp
        assertTrue(spaceEngine.verifyTotpPass(currentPass, secret, currentTime))

        // Valid within 15 seconds clock drift
        assertTrue(spaceEngine.verifyTotpPass(currentPass, secret, currentTime + 15_000L))

        // Invalid for bogus code
        assertFalse(spaceEngine.verifyTotpPass("000000", secret, currentTime))
    }

    @Test
    fun test08_coworkingSpaceTelemetryCapacityCalculation() {
        val telem = spaceEngine.evaluateSpaceTelemetry(
            activeHeadcount = 72,
            rawCo2 = 620,
            rawNoise = 48.5
        )

        assertEquals(72, telem.activeHeadcount)
        assertEquals(48, telem.availableWorkstations) // 120 - 72
        assertEquals(60.0, telem.occupancyRatePct, 0.01)
        assertEquals("EXCELLENT_WELL_VENTILATED", telem.airQualityStatus)
        assertEquals(620, telem.indoorCo2Ppm)
    }

    @Test
    fun test09_hardwareDriverDualEngineStream() = runBlocking {
        val driver = HardwareSensorDriver(rfidEngine, spaceEngine)
        val frame = driver.observePeripheralStream().first()

        assertEquals("CONNECTING", frame.connectionState)
        assertTrue(frame.peripheralName.contains("RFID Turnstile"))
        assertTrue(frame.livePayload.contains("ISO 14443-A"))
    }

    @Test
    fun test10_repositoryCartAndVatCalculation() = runBlocking {
        val sensorDriver = HardwareSensorDriver(rfidEngine, spaceEngine)
        val repository = FadaCoworkRepositoryImpl(sensorDriver)

        val item1 = FadaCoworkItem("cw-1", "اشتراك مكتب مخصص شهري", "مكتب مجهز بالكامل", 1200.0, "معتمد", "الخدمات الرئيسية")
        val item2 = FadaCoworkItem("cw-2", "حجز قاعة اجتماعات تنفيذية (4 ساعات)", "شاشة تفاعلية وعزل صوتي", 400.0, "فوري", "المراقبة الحية")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada Contactless EMV")

        assertEquals(1600.0, order.subtotalSar, 0.01)
        assertEquals(240.0, order.vatSar, 0.01) // 15% VAT
        assertEquals(1840.0, order.totalSar, 0.01)
        assertEquals("Mada Contactless EMV", order.paymentMethod)
        assertEquals(0, repository.activeCart.value.size)
    }
}
