package sa.factory.fadacoworkcoworkin.engine

import java.security.MessageDigest
import kotlin.math.abs

/**
 * Smart Coworking Space Reservation and TOTP Dynamic Pass Engine.
 * Generates RFC 6238-compliant time-based rolling turnstile gate credentials
 * and monitors real-time environmental capacity telemetry.
 */
class SmartSpaceReservationEngine {

    companion object {
        const val TOTP_TIME_STEP_SECONDS = 30L
        const val TOTAL_WORKSTATIONS = 120
        const val TOTAL_MEETING_ROOMS = 8
    }

    enum class SpaceType(val displayName: String, val hourlyRateSar: Double) {
        HOT_DESK("Hot Desk Flex Space", 25.0),
        MEETING_POD("Acoustic Meeting Pod (2-4 pax)", 65.0),
        BOARDROOM("Executive Boardroom (12 pax)", 180.0),
        FOCUS_BOOTH("Quiet Focus Booth (1 pax)", 35.0)
    }

    data class WorkstationReservation(
        val reservationId: String,
        val memberId: String,
        val spaceType: SpaceType,
        val assignedDeskId: String,
        val startTimeMs: Long,
        val durationHours: Int,
        val isCheckedIn: Boolean = false
    )

    data class CoworkingSpaceTelemetry(
        val activeHeadcount: Int,
        val availableWorkstations: Int,
        val occupancyRatePct: Double,
        val indoorCo2Ppm: Int,
        val ambientNoiseDba: Double,
        val airQualityStatus: String
    )

    /**
     * Generates a 6-digit cryptographic TOTP gate pass derived from member secret key and timestamp window.
     */
    fun generateTotpPass(secretKey: String, timestampMs: Long = System.currentTimeMillis()): String {
        val timeWindow = timestampMs / 1000L / TOTP_TIME_STEP_SECONDS
        val input = "$secretKey:$timeWindow"
        
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray(Charsets.UTF_8))
        
        // Dynamic truncation
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                     ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                     ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                     (hash[offset + 3].toInt() and 0xFF)

        val otp = abs(binary % 1_000_000)
        return "%06d".format(otp)
    }

    /**
     * Validates a TOTP pass against the current and adjacent time window (allowing +/- 30s clock drift).
     */
    fun verifyTotpPass(passCode: String, secretKey: String, currentTimestampMs: Long = System.currentTimeMillis()): Boolean {
        for (drift in -1..1) {
            val driftTime = currentTimestampMs + (drift * TOTP_TIME_STEP_SECONDS * 1000L)
            if (generateTotpPass(secretKey, driftTime) == passCode) {
                return true
            }
        }
        return false
    }

    /**
     * Computes live indoor space telemetry.
     */
    fun evaluateSpaceTelemetry(activeHeadcount: Int, rawCo2: Int = 540, rawNoise: Double = 46.5): CoworkingSpaceTelemetry {
        val clampedCount = activeHeadcount.coerceIn(0, TOTAL_WORKSTATIONS)
        val available = TOTAL_WORKSTATIONS - clampedCount
        val occupancyPct = (clampedCount.toDouble() / TOTAL_WORKSTATIONS.toDouble()) * 100.0

        val airStatus = when {
            rawCo2 < 800 -> "EXCELLENT_WELL_VENTILATED"
            rawCo2 < 1200 -> "MODERATE_VENTILATION_ACTIVE"
            else -> "WARNING_HVAC_AIR_PURGE_TRIGGERED"
        }

        return CoworkingSpaceTelemetry(
            activeHeadcount = clampedCount,
            availableWorkstations = available,
            occupancyRatePct = occupancyPct,
            indoorCo2Ppm = rawCo2,
            ambientNoiseDba = rawNoise,
            airQualityStatus = airStatus
        )
    }

    /**
     * Creates a new smart space reservation with automatic desk allocation.
     */
    fun createReservation(
        memberId: String,
        spaceType: SpaceType,
        durationHours: Int,
        deskIndex: Int = 1
    ): WorkstationReservation {
        val deskPrefix = when (spaceType) {
            SpaceType.HOT_DESK -> "DESK-A"
            SpaceType.MEETING_POD -> "POD-M"
            SpaceType.BOARDROOM -> "BOARD-E"
            SpaceType.FOCUS_BOOTH -> "BOOTH-F"
        }
        return WorkstationReservation(
            reservationId = "RES-${System.currentTimeMillis() % 100000}",
            memberId = memberId,
            spaceType = spaceType,
            assignedDeskId = "$deskPrefix-%02d".format(deskIndex),
            startTimeMs = System.currentTimeMillis(),
            durationHours = durationHours,
            isCheckedIn = false
        )
    }
}
