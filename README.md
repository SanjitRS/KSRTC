# Smaran: Assistive Cognitive & Patient Safety Platform

## 📱 Latest APK Downloads (Instant Bridge & Universal Sync)
- **Caregiver App (Guardian)**: [https://files.catbox.moe/oc3u5s.apk](https://files.catbox.moe/oc3u5s.apk)
- **Patient App (Tracker)**: [https://files.catbox.moe/2xburn.apk](https://files.catbox.moe/2xburn.apk)

### Recent Updates:
- **Universal MQTT Bridge**: Caregiver now subscribes to `bmtc_findmy/v2/#`, instantly detecting any Patient device on the HiveMQ broker without requiring manual email typing or prior pairing.
- **Auto-Lock Email Extraction**: Automatically extracts the patient's authenticated email (e.g. `onlysongs746@gmail.com`) directly from MQTT topics and binds telemetry instantly.
- **Baseline Telemetry & Immediate Broadcast**: Generates and broadcasts full cognitive baseline scores immediately upon launch, even before GPS fix or initial games are completed.
- **Direct Email & Demo Login**: Added fallback email entry on the sign-in screen to bypass any Google Play Services account dialog delays.
