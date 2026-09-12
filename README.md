# Smaran: Assistive Cognitive & Patient Safety Platform

## 📱 Latest APK Downloads (Crash Fix & Instant Sync)
- **Caregiver App (Guardian)**: [https://files.catbox.moe/9umw8g.apk](https://files.catbox.moe/9umw8g.apk)
- **Patient App (Tracker)**: [https://files.catbox.moe/kjm1w4.apk](https://files.catbox.moe/kjm1w4.apk)

### Recent Updates:
- **Zero Crash Guarantee**: Wrapped all Android 13/14 notification calls (`NotificationHelper`) and Do Not Disturb audio routines (`SoundPlayer`) in defensive permission guards and try-catches.
- **Unified HiveMQ Relay**: Locked both Caregiver and Patient apps to the canonical `broker.hivemq.com` to eliminate broker split-brain issues.
- **Instant Live GPS & Telemetry Sync**: Real-time continuous location pings and reactive cognitive scores with cross-channel auto-bridging.
