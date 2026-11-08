# تطبيق فضاء كوويرك (`fada-cowork-coworking-1290117`)
> منظومة coworking المتكاملة مع ربط 13.56 MHz RFID Turnstile Access + TOTP Pass

- **Project ID**: `#1290117`
- **Commercial Archetype**: `coworking` (Shared offices, meeting rooms, smart desk reservations)
- **Bound Hardware Peripheral**: `13.56 MHz RFID Turnstile Access + TOTP Pass`
- **Package Name**: `sa.factory.fadacoworkcoworkin`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]

### 1. Architectural & Production Engines
- **`RfidTurnstileAccessProtocolEngine`**:
  * Contactless ISO/IEC 14443-A APDU command processor with MIFARE mutual authentication simulation.
  * Card UID checksum verification (LRC check byte calculation).
  * Hardware flap-gate turnstile access controller (`CMD_GATE_UNLOCK_ENTRY`, `CMD_GATE_UNLOCK_EXIT`).
  * Strict Anti-Passback (APB) ledger and violation blocker (`isAntiPassbackViolation`).
  * Multi-tier access privilege verification (`GUEST`, `HOT_DESK`, `DEDICATED_DESK`, `EXECUTIVE_SUITE`, `FACILITY_ADMIN`).

- **`SmartSpaceReservationEngine`**:
  * Time-based OTP (TOTP) dynamic access pass generator (RFC 6238 compliant 6-digit rolling code, 30-second time window).
  * Cryptographic pass drift verification permitting +/- 30s clock skew.
  * Real-time coworking space capacity management across 120 workstations and 8 meeting rooms.
  * Indoor environmental air quality monitoring ($CO_2$ ppm, ambient noise level $dBA$).
  * Automated desk allocation for hot desks, acoustic pods, and executive boardrooms.

### 2. UI & Driver Modernization
- **`HardwareSensorDriver`**: Dual-engine telemetry loop streaming turnstile flap gate arming, rolling TOTP codes, available workstation counts, and ambient air telemetry into ViewModel StateFlows.
- **`MainActivity` & Compose UI**: Fixed missing typography imports, resolved single-quote template hazards, migrated deprecated `Divider` to `HorizontalDivider`, and eliminated non-standard unicode glyphs (checkmarks replaced with `[OK]`, crosses with `[X]`, bullets with `|`).
- **`FadaCoworkViewModel`**: Enhanced `checkout(paymentMethod: String)` signature for dynamic Mada / Apple Pay POS processing.
- **`AndroidManifest.xml`**: Modernized application label to `فضاء كوويرك - Fada Cowork Access`.

### 3. Unit Regression Test Suite
- Comprehensive 10-case JUnit test suite in `sa.factory.fadacoworkcoworkin.FadaCoworkRegressionTest`:
  * `test01_cardUidChecksumValidation`
  * `test02_apduCommandSerialization`
  * `test03_turnstileAccessGrantedForHotDesk`
  * `test04_antiPassbackEnforcementBlocksDoubleEntry`
  * `test05_guestTurnstileEntryBlocked`
  * `test06_totpRollingPassGeneration`
  * `test07_totpPassDriftVerification`
  * `test08_coworkingSpaceTelemetryCapacityCalculation`
  * `test09_hardwareDriverDualEngineStream`
  * `test10_repositoryCartAndVatCalculation`

### 4. Build and Test Instructions
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Regression Tests
./gradlew testDebugUnitTest
```
