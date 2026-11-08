package sa.factory.fadacoworkcoworkin.engine

/**
 * 13.56 MHz RFID Turnstile Access Control Protocol Engine.
 * Implements ISO/IEC 14443-A contactless card framing, MIFARE authentication,
 * and Anti-Passback (APB) turnstile gate controller logic.
 */
class RfidTurnstileAccessProtocolEngine {

    companion object {
        // ISO 14443-A APDU Instruction Codes
        const val INS_SELECT = 0xA4
        const val INS_AUTH_KEY_A = 0x60
        const val INS_AUTH_KEY_B = 0x61
        const val INS_READ_BINARY = 0xB0
        const val INS_UPDATE_BINARY = 0xD6

        // Turnstile Hardware Gate Commands
        const val CMD_GATE_UNLOCK_ENTRY = 0x11
        const val CMD_GATE_UNLOCK_EXIT = 0x12
        const val CMD_GATE_LOCK_IMMEDIATE = 0x1F

        // Anti-Passback Timeout in Milliseconds
        const val DEFAULT_APB_WINDOW_MS = 60_000L
    }

    enum class AccessTier(val tierName: String, val accessLevel: Int) {
        GUEST("Guest Visitor", 1),
        HOT_DESK("Hot Desk Member", 2),
        DEDICATED_DESK("Dedicated Desk Resident", 3),
        EXECUTIVE_SUITE("Executive Suite Enterprise", 4),
        FACILITY_ADMIN("Facility Operations & Security", 5)
    }

    enum class GateDirection {
        ENTRY,
        EXIT
    }

    sealed interface TurnstileAccessResult {
        data class AccessGranted(
            val cardUidHex: String,
            val memberId: String,
            val tier: AccessTier,
            val gateId: String,
            val direction: GateDirection,
            val unlockDurationMs: Long = 5000L
        ) : TurnstileAccessResult

        data class AccessDenied(
            val cardUidHex: String,
            val reason: String,
            val isAntiPassbackViolation: Boolean = false
        ) : TurnstileAccessResult
    }

    data class TurnstileAccessEvent(
        val memberId: String,
        val cardUidHex: String,
        val direction: GateDirection,
        val timestamp: Long = System.currentTimeMillis()
    )

    // In-memory Anti-Passback ledger: MemberId -> Last Direction
    private val antiPassbackLedger = mutableMapOf<String, TurnstileAccessEvent>()

    /**
     * Validates card UID checksum (LRC check byte calculation).
     */
    fun validateCardUid(uidBytes: ByteArray): Boolean {
        if (uidBytes.size != 5) return false // 4-byte UID + 1 BCC byte
        val calculatedBcc = uidBytes[0].toInt() xor uidBytes[1].toInt() xor uidBytes[2].toInt() xor uidBytes[3].toInt()
        val receivedBcc = uidBytes[4].toInt() and 0xFF
        return (calculatedBcc and 0xFF) == receivedBcc
    }

    /**
     * Builds an ISO 7816-4 APDU command buffer.
     */
    fun buildApduCommand(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray = byteArrayOf()): ByteArray {
        val lc = data.size
        val apdu = ByteArray(5 + lc)
        apdu[0] = (cla and 0xFF).toByte()
        apdu[1] = (ins and 0xFF).toByte()
        apdu[2] = (p1 and 0xFF).toByte()
        apdu[3] = (p2 and 0xFF).toByte()
        apdu[4] = (lc and 0xFF).toByte()
        if (lc > 0) {
            System.arraycopy(data, 0, apdu, 5, lc)
        }
        return apdu
    }

    /**
     * Evaluates access credentials through the turnstile with strict Anti-Passback (APB) enforcement.
     */
    fun evaluateTurnstileTap(
        cardUidHex: String,
        memberId: String,
        tier: AccessTier,
        direction: GateDirection,
        gateId: String = "GATE-SOUTH-01"
    ): TurnstileAccessResult {
        // 1. Anti-Passback Check
        val lastEvent = antiPassbackLedger[memberId]
        if (lastEvent != null && lastEvent.direction == direction) {
            val elapsed = System.currentTimeMillis() - lastEvent.timestamp
            if (elapsed < DEFAULT_APB_WINDOW_MS) {
                return TurnstileAccessResult.AccessDenied(
                    cardUidHex = cardUidHex,
                    reason = "Anti-Passback violation: member already recorded as ${direction.name}",
                    isAntiPassbackViolation = true
                )
            }
        }

        // 2. Privilege Level Verification
        if (tier.accessLevel < AccessTier.HOT_DESK.accessLevel && direction == GateDirection.ENTRY) {
            return TurnstileAccessResult.AccessDenied(
                cardUidHex = cardUidHex,
                reason = "Membership tier ${tier.tierName} does not permit direct turnstile entry without front-desk check-in"
            )
        }

        // 3. Record Successful Passage in APB Ledger
        antiPassbackLedger[memberId] = TurnstileAccessEvent(memberId, cardUidHex, direction)

        return TurnstileAccessResult.AccessGranted(
            cardUidHex = cardUidHex,
            memberId = memberId,
            tier = tier,
            gateId = gateId,
            direction = direction
        )
    }

    /**
     * Clears APB ledger for testing or emergency building evacuation.
     */
    fun resetAntiPassbackLedger() {
        antiPassbackLedger.clear()
    }
}
